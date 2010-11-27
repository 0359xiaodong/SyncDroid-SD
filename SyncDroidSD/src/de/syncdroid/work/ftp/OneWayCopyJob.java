package de.syncdroid.work.ftp;

import java.io.File;
import java.util.Date;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.zehon.BatchTransferProgressDefault;
import com.zehon.FileTransferStatus;
import com.zehon.exception.FileTransferException;
import com.zehon.ftp.FTP;
import com.zehon.ftp.FTPClient;
import com.zehon.ftps.FTPs;
import com.zehon.sftp.SFTP;

import de.syncdroid.AbstractActivity;
import de.syncdroid.Job;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.ProfileService;

public class OneWayCopyJob implements Job {

	private static final String TAG = "FtpCopyJob";

	public static final String ACTION_PROFILE_UPDATE 
		= "de.syncdroid.ACTION_PROFILE_UPDATE";
	
	private Context context;
	private Profile profile;
	private ProfileService profileService;
	
	public OneWayCopyJob(Context context, Profile profile, 
			ProfileService profileService) {
		this.context = context;

        if(profile.getRemotePath().startsWith("/")) {
        	profile.setRemotePath(profile.getRemotePath().substring(1));
        }
        
        Log.i(TAG, "OneWayCopyJob()");
        
        this.profileService = profileService;
        
		this.profile = profile;
	}
	
	private Long findNewestFile(File dir) {
		Long newest = 0L;

		for (String item : dir.list()) {
			Log.d(TAG, " - contains: " + item);
			File fileItem = new File(dir, item);
			if (fileItem.isDirectory()) {
				Long newestInSubdirectory = findNewestFile(fileItem);
				newest = Math.max(newestInSubdirectory, newest);
			} else {
				newest = Math.max(newest, fileItem.lastModified());
			}
		}
		
		return newest;
	}	

	private void updateStatus(String msg) {
		Log.e(TAG, "message: " + msg);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_PROFILE_UPDATE);
        broadcastIntent.putExtra(AbstractActivity.EXTRA_ID, profile.getId());
        broadcastIntent.putExtra(AbstractActivity.EXTRA_MESSAGE, msg);
        this.context.sendBroadcast(broadcastIntent);

/*		
		messageService.sendMessageToClients(SyncService.PROFILE_STATUS_UPDATED, 
				new ProfileHelper(profile.getId(), mgs));
				*/
		
	}

	public void execute() {
		String host = profile.getHostname();
		String username = profile.getUsername();
		String password = profile.getPassword();
		String sendingFolder = profile.getLocalPath();
		String destFolder = profile.getRemotePath();
		
		if(sendingFolder == null) {
			Log.e(TAG, "sendingFolder is null :-(");
			Toast.makeText(context, "invalid localPath for profile '" 
					+ profile.getName() + "'", 2000).show();
			return ;
		}
		
		File file = new File(sendingFolder);

		if(file.exists() == false) {
			updateStatus("file not found: " + sendingFolder);
			Log.e(TAG, "file not found: " + sendingFolder);
			Toast.makeText(context, "localPath not found for profile '" 
					+ profile.getName() + "': " + sendingFolder, 2000).show();
			return ;
		}
		
		Long newestFileAtLocalPath = findNewestFile(file);
		
		if (profile.getLastSync() != null 
				&& newestFileAtLocalPath <= profile.getLastSync().getTime()) {
			Log.d(TAG, "nothing to do");
			return;
		}		
		
		BatchTransferProgressDefault progressCallback = 
			new BatchTransferProgressDefault() {
				public void transferComplete(String fileName) {
					updateStatus("transfer of '" + fileName + "' completed.");
				};
				
				public void transferError(String fileName, Throwable errorException) {
					updateStatus("transfer of '" + fileName + "' failed: "
						+ errorException);
					
				};
				
				public void transferStart(String fileName) {
					updateStatus("transfer of '" + fileName + "' started.");
				};
			};
		
		try {
			int status = 0;
			FTPClient ftpClient = new FTPClient("ftp.myhost.com","ftp", "pass");
			


			
			switch (profile.getProfileType()) {
			case FTP: 
				status = FTP.sendFolder(sendingFolder, destFolder, 
						progressCallback, 
						host, username, password);
				break;
			case FTPs: 
				status = FTPs.sendFolder(sendingFolder, destFolder, 
						progressCallback, 
						host, username, password);
				break;
			case SFTP: 
				status = SFTP.sendFolder(sendingFolder, destFolder, 
						progressCallback, 
						host, username, password);
			}
			
			if(FileTransferStatus.SUCCESS == status){
				Log.d(TAG, sendingFolder + 
						" got ftp-ed successfully to  folder "+destFolder);
				Log.d(TAG, "upload success");

				profile.setLastSync(new Date());
				profileService.update(profile);
			}
			else if(FileTransferStatus.FAILURE == status){
				Log.e(TAG, "Fail to ftp  to  folder "+destFolder);
			}

		} catch (FileTransferException e) {
			Log.e(TAG, "error transfering files: ", e);
		}		
	}

}
