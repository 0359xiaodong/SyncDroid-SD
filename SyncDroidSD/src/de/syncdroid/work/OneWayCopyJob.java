package de.syncdroid.work;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import de.syncdroid.ProfileStatusLevel;
import de.syncdroid.R;
import de.syncdroid.Utils;
import de.syncdroid.activity.ProfileListActivity;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.transfer.FileTransferClient;
import de.syncdroid.transfer.impl.FtpFileTransferClient;
import de.syncdroid.transfer.impl.ScpFileTransferClient;
import de.syncdroid.transfer.impl.SmbFileTransferClient;
import de.syncdroid.work.AbstractCopyJob;

public class OneWayCopyJob extends AbstractCopyJob implements Runnable {
	private static final String TAG = "FtpCopyJob";

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

				if(!fileTransferClient.transfer(item.source, Utils.combinePath(item.fullpath, item.name))) {
                    updateStatus("error transfering file", ProfileStatusLevel.ERROR, item.fullpath);
                    Log.e(TAG, "error transfering file '" + item.fullpath + "'");
                }
				transferedFiles  ++;

				String msg = "transfering ... uploaded " + transferedFiles + "/" + filesToTransfer;
				Log.d(TAG, msg);

			    updateStatus(msg, ProfileStatusLevel.INFO, item.name);
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
						new Intent(context, ProfileListActivity.class), 0);

				notification.setLatestEventInfo(context,
						"upload in progress", msg, contentIntent);
				notificationManager.notify(R.string.remote_service_started, notification);
			} else {
				filesToTransfer --;
			}
		}
	}


	public void run() {
		Log.d(TAG, "lastSync: " + profile.getLastSync());

        if(testRunCondition() == false) {
            return;
        }
		
		updateStatus("checking for file status ...", ProfileStatusLevel.INFO, "");
		
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
				updateStatus("file not found: ", ProfileStatusLevel.ERROR, path);
				Log.e(TAG, "file not found: " + path);
				Toast.makeText(context, "localPath not found for profile '" 
						+ profile.getName() + "': " + path, 2000).show();
				return ;
			}

			transferedFiles = 0;
			filesToTransfer = 0;
			RemoteFile rootRemote = buildTree(file, profile.getRemotePath(), profile.getLastSync().getTime());
			
			if (profile.getLastSync() != null 
					&& rootRemote.newest <= profile.getLastSync().getTime()) {
				Log.d(TAG, "nothing to do");
				updateStatus("nothing to do", ProfileStatusLevel.SUCCESS, "");
				return;
			}

			notification = new Notification(R.drawable.icon, 
					"upload started for '" + profile.getName() + "'",
					System.currentTimeMillis());
			

			// The PendingIntent to launch our activity if the user selects this
			// notification
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
					new Intent(context, ProfileListActivity.class), 0);
			notificationManager = 
				(NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);

			// Set the info for the views that show in the notification panel.
			notification.setLatestEventInfo(context,
					"upload started", "starting uploading ...", contentIntent);
			
			notificationManager.notify(R.string.remote_service_started, notification);
			
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
				fileTransferClient = new FtpFileTransferClient(profile.getHostname(),
						profile.getUsername(), profile.getPassword());
				break;
			case SCP:
				fileTransferClient = new ScpFileTransferClient(profile.getHostname(),
						profile.getUsername(), profile.getPassword(), context);
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

			uploadFiles(rootRemote, fileTransferClient, profile.getLastSync() != null ? 
					profile.getLastSync().getTime() : null);

			Long transferFinish = System.currentTimeMillis();
			
			Long transferTimeSeconds = (transferFinish - transferBegin) / 1000;
			
			String msg = "finished. uploaded " + transferedFiles + "/" 
				+ filesToTransfer + " in " + transferTimeSeconds + " seconds";
			Log.d(TAG, msg);

			notification.setLatestEventInfo(context,
					"upload success", msg, contentIntent);
			
			notificationManager.notify(R.string.remote_service_started, notification);
            notification.tickerText = msg;

            profile = profileService.findById(profile.getId());
			profile.setLastSync(new Date());
			profileService.update(profile);
			
			if(!fileTransferClient.disconnect()) {
			    Log.e(TAG, "failed to disconnect :-(");
            }
		} catch(Exception e) {
			Log.e(TAG, "whoa, exception: ", e);
			updateStatus("error", ProfileStatusLevel.ERROR, e.toString());
			
			Toast.makeText(context, e.toString(), 2000).show();
			return;
		}		

	}

}
