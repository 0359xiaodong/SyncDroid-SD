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

import com.google.inject.Inject;

import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.ProfileService;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

public class FtpCopyJob implements Job {
	private static final String TAG = "FtpCopyJob";
	
	private Context context;
	private Profile profile;
	private ProfileService profileService;
	
	public FtpCopyJob(Context context, Profile profile, 
			ProfileService profileService) {
		this.context = context;

        if(profile.getRemotePath().startsWith("/")) {
        	profile.setRemotePath(profile.getRemotePath().substring(1));
        }
        
        this.profileService = profileService;
        
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
		Log.d(TAG, "entering " + dir.getAbsolutePath());
		RemoteFile here = new RemoteFile();
		here.isDirectory = true;
		here.name = dir.getName();
		here.source = dir;
		here.newest = 0L;

		for (String item : dir.list()) {
			Log.d(TAG, " - contains: " + item);
			File fileItem = new File(dir, item);
			if (fileItem.isDirectory()) {
				RemoteFile tmp = buildTree(fileItem);
				here.children.add(tmp);
				Log.d(TAG, " - adding: " + tmp.name);
				here.newest = Math.max(here.newest, tmp.newest);
			} else {
				RemoteFile aFile = new RemoteFile();
				aFile.isDirectory = false;
				aFile.name = item;
				aFile.source = fileItem;
				here.children.add(aFile);
				Log.d(TAG, " - adding: " +aFile.name);
				here.newest = Math.max(here.newest, fileItem.lastModified());
			}
		}
		
		return here;
	}

	private void uploadFiles(RemoteFile dir, FTPClient ftpClient, Long lastUpload) throws IOException {
		Log.d(TAG, "uploading file: " + dir.name);
		if (!ftpClient.changeWorkingDirectory(dir.name)) {
			if (!ftpClient.makeDirectory(dir.name)) {
				Log.e(TAG, "could not create directory: " + dir.name);
				return;
			} else if (!ftpClient.changeWorkingDirectory(dir.name)) {
				Log.e(TAG, "could not change directory" + dir.name);
				return;
			}
		}
		
		for (RemoteFile item : dir.children) {
			Log.d(TAG, "looking at " + item.name);
			if (item.isDirectory) {
				uploadFiles(item, ftpClient, lastUpload);
			} else if (lastUpload == null || ( 
					item.source != null && item.source.lastModified() 
					> lastUpload)) {
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
		Log.d(TAG, "lastSync: " + profile.getLastSync());
		try {
			String path = profile.getLocalPath();
			if(path == null) {
				Log.e(TAG, "path is null :-(");
				Toast.makeText(context, "invalid path for profile '" 
						+ profile.getName() + "'", 2000).show();
				return ;
			}
			
			File file = new File(path);

			if(file.exists() == false) {
				Log.e(TAG, "file not found: " + path);
				Toast.makeText(context, "localPath not found for profile '" 
						+ profile.getName() + "': " + path, 2000).show();
				return ;
			}
			
			RemoteFile rootRemote = buildTree(file);
			
			if (profile.getLastSync() != null 
					&& rootRemote.newest <= profile.getLastSync().getTime()) {
				Log.d(TAG, "nothing to do");
				return;
			}

			// connect to ftp server
			FTPClient ftpClient = new FTPClient();
			ftpClient.connect(InetAddress.getByName(profile.getHostname()));
			if(!ftpClient.login(profile.getUsername(), profile.getPassword())) {
				Log.e(TAG, "login failed for profile '" + profile.getName() + "'");
				Toast.makeText(context, "login failed for profile '" 
						+ profile.getName() + "'", 2000).show();
				
				// disconnect from ftp server
				ftpClient.logout();
				ftpClient.disconnect();
				return ;
			}
			
			
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

			uploadFiles(rootRemote, ftpClient, profile.getLastSync() != null ? 
					profile.getLastSync().getTime() : null);
			
			// disconnect from ftp server
			ftpClient.logout();
			ftpClient.disconnect();
		} catch(Exception e) {
			Log.e(TAG, "whoa, exception: ", e);
			
			Toast.makeText(context, e.toString(), 2000).show();
			return;
		}

		
		
		Log.d(TAG, "upload success");

		profile.setLastSync(new Date());
		profileService.update(profile);
	}

}
