package de.syncdroid.work;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import de.syncdroid.AbstractActivity;
import de.syncdroid.ProfileStatusLevel;
import de.syncdroid.Utils;
import de.syncdroid.db.model.Location;
import de.syncdroid.db.model.LocationCell;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: arturh
 * Date: 27.11.10
 * Time: 23:17
 * To change this template use File | Settings | File Templates.
 */
public class AbstractCopyJob {
	private static final String TAG = "FtpCopyJob";

	public static final String ACTION_PROFILE_UPDATE
		= "de.syncdroid.ACTION_PROFILE_UPDATE";

	protected Integer transferedFiles;
	protected Integer filesToTransfer;
	protected Profile profile;
	protected Context context;
	protected ProfileService profileService;
	protected LocationService locationService;

	protected Notification notification;
	protected NotificationManager notificationManager;

    protected class RemoteFile {
        public Boolean isDirectory;
        public String name;
        public String fullpath;
        public File source;
        public Long newest;
        public List<RemoteFile> children = new ArrayList<OneWayCopyJob.RemoteFile>();
    }

    protected String nicePeriod(Long period) {
        if(period < 1000) {
            return "1s";
        }
        return "";
    }

    protected boolean testRunCondition() {
		if(profile.getOnlyIfWifi()) {
			Log.d(TAG, "checking for wifi");
			WifiManager wifiManager = (WifiManager)
				context.getSystemService(Activity.WIFI_SERVICE);

			if(wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {

				Log.d(TAG, "wifi is off");
				updateStatus("wifi is off", ProfileStatusLevel.WARN, "");
				return false;
			} else {
				Log.d(TAG, "wifi is on");
			}
		}

		if(profile.getLocation() != null) {
			Log.d(TAG, "checking location '"
					+ profile.getLocation().getName() + "'");
			updateStatus("checking location '"
					+ profile.getLocation().getName() + "'", ProfileStatusLevel.INFO, "");

			TelephonyManager tm = (TelephonyManager)
					context.getSystemService(Activity.TELEPHONY_SERVICE);
	        GsmCellLocation location = (GsmCellLocation) tm.getCellLocation();

	        List<Location> locations =
	        	locationService.locate(location.getCid(), location.getLac());
			Log.d(TAG, "at cell '" + new LocationCell(location.getCid(),
					location.getLac()) + "'");

	        if(locations.contains(profile.getLocation()) == false) {
	        	updateStatus("not at location '" + profile.getLocation().getName() + "'", ProfileStatusLevel.WARN, "");
				Log.d(TAG, "not at location '" + profile.getLocation().getName() + "'");

				for(LocationCell cell : profile.getLocation().getLocationCells()) {
					Log.d(TAG, " - " + cell);
				}

				return false;
	        } else {
				Log.d(TAG, "at location '"
						+ profile.getLocation().getName() + "'");
	        }
		}

        return true;
    }

	protected void updateStatus(String msg, ProfileStatusLevel level, String detailMessage) {
		Log.d(TAG, "message: " + msg);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_PROFILE_UPDATE);
        broadcastIntent.putExtra(AbstractActivity.EXTRA_ID, profile.getId());
        broadcastIntent.putExtra(AbstractActivity.EXTRA_MESSAGE, msg);
        broadcastIntent.putExtra(AbstractActivity.EXTRA_LEVEL, level.toString());
        broadcastIntent.putExtra(AbstractActivity.EXTRA_DETAILMESSAGE, detailMessage);
        this.context.sendBroadcast(broadcastIntent);
	}

    protected RemoteFile buildTree(File dir, String fullpath, Long newerThan) {
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
                RemoteFile tmp = buildTree(fileItem, Utils.combinePath(fullpath, item), newerThan);
                here.children.add(tmp);
                Log.d(TAG, " - adding: " + tmp.name);
                here.newest = Math.max(here.newest, tmp.newest);
            } else {
                RemoteFile aFile = new RemoteFile();
                aFile.isDirectory = false;
                aFile.name = item;
                aFile.source = fileItem;
                aFile.fullpath = fullpath;

                if(newerThan == null || fileItem.lastModified() > newerThan) {
                    here.children.add(aFile);
                    filesToTransfer ++;
                    here.newest = Math.max(here.newest, fileItem.lastModified());
                }
            }
        }

        return here;
    }

}
