package de.syncdroid.work.ftp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zehon.FileTransferClient;
import com.zehon.exception.FileTransferException;
import com.zehon.ftp.FTPClient;
import com.zehon.ftps.FTPsClient;
import com.zehon.sftp.SFTPClient;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;
import de.syncdroid.AbstractActivity;
import de.syncdroid.R;
import de.syncdroid.activity.ProfileListActivity;
import de.syncdroid.db.model.Location;
import de.syncdroid.db.model.LocationCell;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;

public class OneWayFtpCopyJob implements Runnable {
	private static final String TAG = "FtpCopyJob";
	
	public static final String ACTION_PROFILE_UPDATE 
		= "de.syncdroid.ACTION_PROFILE_UPDATE";

	
	
	private Context context;
	private Profile profile;
	private ProfileService profileService;
	private LocationService locationService;
	
	private Integer transferedFiles;
	private Integer filesToTransfer;
	private Notification notification;
	private NotificationManager notificationManager;
	
	public OneWayFtpCopyJob(Context context, Profile profile, 
			ProfileService profileService, LocationService locatonService) {
		this.context = context;

        if(profile.getRemotePath().startsWith("/")) {
        	profile.setRemotePath(profile.getRemotePath().substring(1));
        }
        
        this.profileService = profileService;
        this.locationService = locatonService;
        
		this.profile = profile;
	}

	private class RemoteFile {
		public Boolean isDirectory;
		public String name;
		public String fullpath;
		public File source;
		public Long newest;
		public List<RemoteFile> children = new ArrayList<OneWayFtpCopyJob.RemoteFile>();
	}
	
	private RemoteFile buildTree(File dir, String fullpath) {
        Log.i(TAG, "buildTree with fullpath: '" + fullpath + "'");
		RemoteFile here = new RemoteFile();
		here.isDirectory = true;
		here.name = dir.getName();
		here.fullpath = fullpath;
		here.source = dir;
		here.newest = 0L;

		for (String item : dir.list()) {
			File fileItem = new File(dir, item);
			if (fileItem.isDirectory()) {
				RemoteFile tmp = buildTree(fileItem, fullpath + "/" + item);
				here.children.add(tmp);
				Log.d(TAG, " - adding: " + tmp.name);
				here.newest = Math.max(here.newest, tmp.newest);
			} else {
				RemoteFile aFile = new RemoteFile();
				aFile.isDirectory = false;
				aFile.name = item;
				aFile.source = fileItem;
				aFile.fullpath = fullpath;
				here.children.add(aFile);
				filesToTransfer ++;
				here.newest = Math.max(here.newest, fileItem.lastModified());
			}
		}
		
		return here;
	}

	private void uploadFiles(RemoteFile dir, FileTransferClient fileTransferClient, Long lastUpload) throws IOException, FileTransferException {
		Log.d(TAG, "beginning sync of " + dir.name);
		
		/*
		if (!fileTransferClient.changeWorkingDirectory(dir.name)) {
			if (!fileTransferClient.makeDirectory(dir.name)) {
				Log.e(TAG, "could not create directory: " + dir.name);
				return;
			} else if (!fileTransferClient.changeWorkingDirectory(dir.name)) {
				Log.e(TAG, "could not change directory" + dir.name);
				return;
			}
		}
		*/
		
		for (RemoteFile item : dir.children) {
			updateStatus("uploading: " + item.name);		
			Log.d(TAG, "uploading" + item.name);
			
			if (item.isDirectory) {
				uploadFiles(item, fileTransferClient, lastUpload);
			} else if (lastUpload == null || ( 
					item.source != null && item.source.lastModified() 
					> lastUpload)) {
				BufferedInputStream inputStream=null;
				inputStream = new BufferedInputStream(
						new FileInputStream(item.source));
				//fileTransferClient.enterLocalPassiveMode();

                Log.i(TAG, "transfering '" + item.source + "' to '" + item.fullpath + "'");

				fileTransferClient.sendFile(item.source, item.fullpath);
				transferedFiles  ++;

				String msg = "transfering ... uploaded " + transferedFiles + "/" 
					+ filesToTransfer;
				Log.d(TAG, msg);
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
						new Intent(context, ProfileListActivity.class), 0);

				// Set the info for the views that show in the notification panel.
				notification.setLatestEventInfo(context,
						"upload success", msg, contentIntent);
				notificationManager.notify(R.string.remote_service_started, notification);
				inputStream.close();
			} else {
				filesToTransfer --;
			}
		}
		//fileTransferClient.changeToParentDirectory();
	}
	
	private void updateStatus(String msg) {
		Log.d(TAG, "message: " + msg);
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

	public void run() {
		Log.d(TAG, "lastSync: " + profile.getLastSync());
		
		if(profile.getOnlyIfWifi()) {
			Log.d(TAG, "checking for wifi");
			WifiManager wifiManager = (WifiManager) 
				context.getSystemService(Activity.WIFI_SERVICE);
			
			if(wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {

				Log.d(TAG, "wifi is off");
				updateStatus("wifi is off");
				return;
			} else {
				Log.d(TAG, "wifi is on");
			}			
		}
		
		if(profile.getLocation() != null) {
			Log.d(TAG, "checking location '" 
					+ profile.getLocation().getName() + "'");
			updateStatus("checking location '" 
					+ profile.getLocation().getName() + "'");
			
			TelephonyManager tm = (TelephonyManager) 
					context.getSystemService(Activity.TELEPHONY_SERVICE); 
	        GsmCellLocation location = (GsmCellLocation) tm.getCellLocation();
	        
	        List<Location> locations = 
	        	locationService.locate(location.getCid(), location.getLac());
			Log.d(TAG, "at cell '" + new LocationCell(location.getCid(), 
					location.getLac()) + "'");
	        
	        if(locations.contains(profile.getLocation()) == false) {
	        	updateStatus("not at location '" + profile.getLocation().getName() + "'");
				Log.d(TAG, "not at location '" + profile.getLocation().getName() + "'");
				
				for(LocationCell cell : profile.getLocation().getLocationCells()) {
					Log.d(TAG, " - " + cell);
				}
				
				return;
	        } else {
				Log.d(TAG, "at location '" 
						+ profile.getLocation().getName() + "'");
	        }
		}
		
		updateStatus("checking for file status ...");
		
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
				updateStatus("file not found: " + path);
				Log.e(TAG, "file not found: " + path);
				Toast.makeText(context, "localPath not found for profile '" 
						+ profile.getName() + "': " + path, 2000).show();
				return ;
			}

			transferedFiles = 0;
			filesToTransfer = 0;
			RemoteFile rootRemote = buildTree(file, profile.getRemotePath());
			
			if (profile.getLastSync() != null 
					&& rootRemote.newest <= profile.getLastSync().getTime()) {
				Log.d(TAG, "nothing to do");
				updateStatus("nothing to do");
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
					"upload success", "starting uploading ...", contentIntent);
			
			notificationManager.notify(R.string.remote_service_started, notification);
			
			Long transferBegin = System.currentTimeMillis();

			FileTransferClient fileTransferClient = null;
			
			switch (profile.getProfileType()) {
			case FTP: 
				fileTransferClient = new FTPClient(profile.getHostname(), 
						profile.getUsername(), profile.getPassword());
				break;
			case FTPs: 
				fileTransferClient = new FTPsClient(profile.getHostname(), 
						profile.getUsername(), profile.getPassword(), true);
				break;
			case SFTP: 
				fileTransferClient = new SFTPClient(profile.getHostname(), 
						profile.getUsername(), profile.getPassword());
				break;
			}

			
			/*fileTransferClient.connect(InetAddress.getByName(profile.getHostname()));
			if(!fileTransferClient.login(profile.getUsername(), profile.getPassword())) {
				Log.e(TAG, "login failed for profile '" + profile.getName() + "'");
				Toast.makeText(context, "login failed for profile '" 
						+ profile.getName() + "'", 2000).show();
				

				updateStatus("login failed");
				// disconnect from ftp server
				fileTransferClient.logout();
				fileTransferClient.disconnect();
				return ;
			}*/
			
			
			//fileTransferClient.setFileType(FTP.BINARY_FILE_TYPE);

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

			profile.setLastSync(new Date());
			profileService.update(profile);
			
			// disconnect from ftp server
			/*ftpClient.logout();
			ftpClient.disconnect();*/
			//FileTransferClient.closeCache();
		} catch(Exception e) {
			Log.e(TAG, "whoa, exception: ", e);
			updateStatus("error");
			
			Toast.makeText(context, e.toString(), 2000).show();
			return;
		}		

	}

}
