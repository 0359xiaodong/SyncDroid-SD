package de.syncdroid.work;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import de.syncdroid.db.model.enums.ProfileStatusLevel;
import de.syncdroid.R;
import de.syncdroid.Utils;
import de.syncdroid.activity.ProfileListActivity;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.model.enums.ProfileStatusType;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.transfer.FileTransferClient;
import de.syncdroid.transfer.impl.DropboxFileTransferClient;
import de.syncdroid.transfer.impl.FtpFileTransferClient;
import de.syncdroid.transfer.impl.ScpFileTransferClient;
import de.syncdroid.transfer.impl.SmbFileTransferClient;

public class OneWayCopyJob extends AbstractCopyJob implements Runnable {	
	private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");

    private static final Integer MIN_FILE_AGE = 3000;

	public OneWayCopyJob(Context context, Profile profile,
                         ProfileService profileService, LocationService locatonService) {
		this.context = context;
        this.profileService = profileService;
        this.locationService = locatonService;
		this.profile = profile;
	}

	private void uploadFiles(RemoteFile dir, FileTransferClient fileTransferClient, Long lastUpload) throws IOException {
		Log.d(TAG, "beginning sync of " + dir.name);
		
		for (RemoteFile item : dir.children) {
			if (item.isDirectory) {
				uploadFiles(item, fileTransferClient, lastUpload);
			} else if (lastUpload == null || ( 
					item.source != null && item.source.lastModified() 
					> lastUpload)) {
                Log.i(TAG, "transfering '" + item.source + "' to '" + item.fullpath + "'");

                String msg = "transfering file " + (transferedFiles+1) + "/" + filesToTransfer;
				Log.d(TAG, msg);

			    updateStatus(msg, ProfileStatusLevel.INFO, item.name, ProfileStatusType.FILE_COPIED,
                        item.source.getAbsolutePath());

				PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
						new Intent(context, ProfileListActivity.class), 0);

				notification.setLatestEventInfo(context,
						"upload in progress", msg, contentIntent);
				notificationManager.notify(R.string.remote_service_started, notification);
				
				if(!fileTransferClient.transfer(item.source, Utils.combinePath(item.fullpath, item.name))) {
                    updateStatus("error transfering file", ProfileStatusLevel.ERROR, item.fullpath);
                    Log.e(TAG, "error transfering file '" + item.fullpath + "'");
                }
				transferedFiles  ++;
			} else {
				filesToTransfer --;
			}
		}
	}


	public void run() {
		Log.d(TAG, "lastSync: " + profile.getLastSync());

		if(profile.getEnabled() == false) {
			updateStatus("disabled", ProfileStatusLevel.INFO, "");
			return;
		}
		
        if(testRunCondition() == false) {
            return;
        }

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				new Intent(context, ProfileListActivity.class), 0);
		
		//updateStatus("checking for file status ...", ProfileStatusLevel.INFO, "");
		
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
				updateStatus("file not found: '" + path + "'", ProfileStatusLevel.ERROR, path);
				Log.e(TAG, "file not found: '" + path + "'");
				Toast.makeText(context, "localPath not found for profile '" 
						+ profile.getName() + "': " + path, 2000).show();
				return ;
			}

			transferedFiles = 0;
			filesToTransfer = 0;
			
			String remotePath = profile.getRemotePath();
			Date lastSync = profile.getLastSync();
			
			RemoteFile rootRemote = buildTree(file, remotePath, 
					lastSync != null ? lastSync.getTime() : null);
			
			if (profile.getLastSync() != null 
					&& rootRemote.newest <= profile.getLastSync().getTime()) {
				Log.d(TAG, "nothing to do");

				String msg = "last successful sync was at " 
						+ timeFormatter.format(profile.getLastSync());
				
				Log.d(TAG, msg);
				
				updateStatus("up-to-update", ProfileStatusLevel.SUCCESS, msg);
				return;
			}

            if(new Date().getTime() - rootRemote.newest < MIN_FILE_AGE) {
                Log.w(TAG, "found file which was changed very recently " +
                        "- aborting this tick");
                return ;
            }

			notification = new Notification(R.drawable.progress, 
					"upload started for '" + profile.getName() + "'",
					System.currentTimeMillis());
			

			notificationManager = 
				(NotificationManager) context.getSystemService(
						Activity.NOTIFICATION_SERVICE);

			// Set the info for the views that show in the notification panel.
			notification.setLatestEventInfo(context,
					"upload started", "starting uploading ...", contentIntent);

			updateStatus("upload started", ProfileStatusLevel.INFO, "");
			
			notificationManager.notify(R.string.remote_service_started, 
					notification);
			
			Long transferBegin = System.currentTimeMillis();

			FileTransferClient fileTransferClient = null;

            if(profile.getProfileType() == null) {
			    Log.e(TAG, "invalid profile type");
                updateStatus("unsupported profile type",
                        ProfileStatusLevel.ERROR, "");
                return ;
            }

			switch (profile.getProfileType()) {
			case FTP:
				fileTransferClient = new FtpFileTransferClient(
						profile.getHostname(), profile.getUsername(), 
						profile.getPassword(), profile.getPort());
				break;
			case SCP:
				fileTransferClient = new ScpFileTransferClient(
						profile.getHostname(), profile.getUsername(), 
						profile.getPassword(), profile.getPort(), null);
				break;
			case SMB:
                String domain = null;
                String username = null;

                if(profile.getUsername().indexOf("\\") != -1) {
                    domain = profile.getUsername().substring(0, profile.getUsername().indexOf("\\"));
                    username = profile.getUsername().substring(profile.getUsername().indexOf("\\") + 1);
                } else {
                    domain = "";
                    username = profile.getUsername();
                }

				fileTransferClient = new SmbFileTransferClient(profile.getHostname(),
						domain, username, profile.getPassword());
				break;
			case DROPBOX: 
				fileTransferClient = new DropboxFileTransferClient(context);
				break;
            default:
			    Log.e(TAG, "unsupported profile type: '" + profile.getProfileType().toString() + "'");
                updateStatus("unsupported profile type: '" + profile.getProfileType().toString() + "'",
                        ProfileStatusLevel.ERROR, "");
                return ;
			}

            if(!fileTransferClient.connect()) {
			    Log.e(TAG, "failed to connect :-(");
                updateStatus("failed to connect", ProfileStatusLevel.ERROR, "");
                return ;
            }

            // this does all the hard work
			uploadFiles(rootRemote, fileTransferClient, profile.getLastSync() != null ? 
					profile.getLastSync().getTime() : null);

			Long transferFinish = System.currentTimeMillis();
			
			Long transferTimeSeconds = (transferFinish - transferBegin) / 1000;
			
			Date now = new Date();
			
			String msg = "finished at " + timeFormatter.format(now) + ". uploaded " 
				+ transferedFiles + "/" 
				+ filesToTransfer + " in " + transferTimeSeconds + " seconds";
			Log.d(TAG, msg);
			
			updateStatus("success", ProfileStatusLevel.SUCCESS, msg);

			notification.setLatestEventInfo(context,
					"upload success", msg, contentIntent);
            notification.icon = R.drawable.success;
            
			notificationManager.notify(R.string.remote_service_started, 
					notification);

            profile = profileService.findById(profile.getId());
			profile.setLastSync(new Date());
			profileService.update(profile);
			
			if(!fileTransferClient.disconnect()) {
			    Log.e(TAG, "failed to disconnect :-(");
            }
		} catch(Exception e) {
			Log.e(TAG, "whoa, exception: ", e);
			updateStatus("error", ProfileStatusLevel.ERROR, e.toString());
			
			
			notification.setLatestEventInfo(context,
					"upload failure", "exception catched", contentIntent);
            notification.icon = R.drawable.error;
            
			notificationManager.notify(R.string.remote_service_started, 
					notification);
			
            notification.icon = R.drawable.success;
			
			Toast.makeText(context, e.toString(), 2000).show();
			return;
		}		

	}

}
