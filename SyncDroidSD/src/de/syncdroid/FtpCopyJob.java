package de.syncdroid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import de.syncdroid.db.model.Profile;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class FtpCopyJob implements Job {
	private static final String TAG = "FtpCopyJob";
	
	private Long lastSync;
	private Context context;
	private Profile profile;
	
	public static final String PREF_FTP_PATH = "pref_ftp_path";
	public static final String PREF_FTP_PASSWORD = "pref_ftp_password";
	public static final String PREF_FTP_USERNAMAE = "pref_ftp_username";
	public static final String PREF_FTP_HOST = "pref_ftp_host";
	public static final String PREF_LOCAL_DIRECTORY = "pref_local_directory";
	public static final String PREF_LASTSYNC = "pref_last_sync";
	
	public FtpCopyJob(Context context, Profile profile) {
		this.context = context;

        if(profile.getRemotePath().startsWith("/")) {
        	profile.setRemotePath(profile.getRemotePath().substring(1));
        }
        
		this.profile = profile;
	}

	private class RemoteFile {
		public Boolean isDirectory;
		public String name;
		public File source;
		public Long newest;
		public List<RemoteFile> children = new ArrayList<FtpCopyJob.RemoteFile>();
	}
	
	private RemoteFile buildTree(File dir) {
		RemoteFile here = new RemoteFile();
		here.isDirectory = true;
		here.name = dir.getName();
		here.source = dir;
		here.newest = 0L;

		for (String item : dir.list()) {
			File fileItem = new File(dir, item);
			if (fileItem.isDirectory()) {
				RemoteFile tmp = buildTree(fileItem);
				here.children.add(tmp);
				here.newest = Math.max(here.newest, tmp.newest);
			} else {
				RemoteFile aFile = new RemoteFile();
				aFile.isDirectory = false;
				aFile.name = item;
				aFile.source = fileItem;
				here.children.add(aFile);
				here.newest = Math.max(here.newest, fileItem.lastModified());
			}
		}
		
		return here;
	}

	private void uploadFiles(RemoteFile dir, FTPClient ftpClient, Long lastUpload) throws IOException {
		if (!ftpClient.changeWorkingDirectory(dir.name)) {
			if (!ftpClient.makeDirectory(dir.name)) {
				Log.e(TAG, "could not create directory");
				return;
			} else if (!ftpClient.changeWorkingDirectory(dir.name)) {
				Log.e(TAG, "could not change directory");
				return;
			}
		}
		
		for (RemoteFile item:dir.children) {
			if (item.isDirectory) {
				uploadFiles(item, ftpClient, lastUpload);
			} else if (item.source.lastModified() > lastUpload) {
				BufferedInputStream inputStream=null;
				inputStream = new BufferedInputStream(
						new FileInputStream(item.source));
				ftpClient.enterLocalPassiveMode();
				ftpClient.storeFile(item.name, inputStream);
				inputStream.close();
			}
		}
		ftpClient.changeToParentDirectory();
	}
	
	@Override
	public void execute() {
		Log.d(TAG, "lastSync: " + lastSync);
		try {
			RemoteFile rootRemote = buildTree(new File(profile.getLocalPath()));

			if (lastSync > 0 && rootRemote.newest <= lastSync) {
				Log.d(TAG, "nothing to do");
				return;
			}

			// connect to ftp server
			FTPClient ftpClient = new FTPClient();
			ftpClient.connect(InetAddress.getByName(profile.getHostname()));
			ftpClient.login(profile.getUsername(), profile.getPassword());
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

			uploadFiles(rootRemote, ftpClient, lastSync);
			
			// disconnect from ftp server
			ftpClient.logout();
			ftpClient.disconnect();
		} catch(Exception e) {
			Log.e(TAG, "whoa, exception: " + e);
			
			Toast.makeText(context, e.toString(), 2000);
		}

		Log.d(TAG, "upload success");
	    SharedPreferences.Editor ed = context.getSharedPreferences(ProfileEditActivity.TAG, Activity.MODE_PRIVATE).edit();
	    ed.putLong(FtpCopyJob.PREF_LASTSYNC, new Date().getTime());
	    ed.commit();
	}

}
