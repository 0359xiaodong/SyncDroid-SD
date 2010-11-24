package de.syncdroid.service;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.inject.Inject;

import de.syncdroid.Job;
import de.syncdroid.MessageService;
import de.syncdroid.R;
import de.syncdroid.SyncBroadcastReceiver;
import de.syncdroid.activity.ProfileListActivity;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.work.ftp.OneWayCopyJob;

public class SyncService extends MessageService {
	private static final String TAG = "SyncService";
	private static final int POLL_INTERVALL = 15000;
	
	public static final String TIMER_TICK = "de.syncdroid.TIMER_TICK";
	public static final String INTENT_START_TIMER = "de.syncdroid.INTENT_START_TIMER";
	
	@Inject private ProfileService profileService;
	private Boolean currentlyRunning = false;

    /** For showing and hiding our notification. */
    private NotificationManager mNM;

    public static final int PROFILE_STATUS_UPDATED = 3;

    @Override
    public void onStart(Intent intent, int startId) {
    	super.onStart(intent, startId);
		// handle intents
		if( intent != null && intent.getAction() != null ) 
		{
			if( intent.getAction().equals(TIMER_TICK)  )
			{
				Log.d(TAG, "TIMER_TICK");
				if(currentlyRunning) {
					Log.w(TAG, "WARNING: TIMER_TICKET while running syncIt");
				} else {
					currentlyRunning = true;
					syncIt();
					currentlyRunning = false;
				}
			}
			else if(intent.getAction().equals(INTENT_START_TIMER) ||
				intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
			{
				Log.d(TAG, "set timer");
				AlarmManager mgr=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
				Intent i=new Intent(this, SyncBroadcastReceiver.class);
				i.setAction(TIMER_TICK);
				
				// get a Calendar object with current time
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, 4);

				PendingIntent pi=PendingIntent.getBroadcast(this, 0, i, 0);
				mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
						SystemClock.elapsedRealtime(), POLL_INTERVALL, pi);

			}
			} else {
				Log.d(TAG, "unknown intent:");
				Log.d(TAG, "Receive intent= " + intent );
				Log.d(TAG, "action= " + intent.getAction() );
			}
		}
    

	
	    
	@Override
	public void handleRegisterClient() {
		//sendMessageToClients(FOUND_NEW_CELL, currentCellLocation);
		
	}
	
	@Override
	public void handleUnregisterClient() {
		
	}
    @Override
    public boolean handleMessage(Message msg) {
    	if(super.handleMessage(msg)) {
    		return true;
    	}
    	
    	Log.d(TAG, "msg.what: " + msg.what);
        switch (msg.what) {
            default:
                return false;
        }
    }

            
    private void syncIt() {
		Log.d(TAG, "syncIt()");

		if(profileService != null) {
			for(Profile profile : profileService.list()) {
				Job job = 
					new OneWayCopyJob(this, profile, profileService, this);
				job.execute();
			}
		} else {
			Log.e(TAG, "profileService is NULL");
		}
		
	}



    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.
        showNotification();
    }

    @Override
    public void onDestroy() {
    	Log.d(TAG,"onDestroy()");
    	
        // Cancel the persistent notification.
        mNM.cancel(R.string.remote_service_started);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    }



    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.remote_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ProfileListActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.remote_service_started, notification);
    }
	
}
