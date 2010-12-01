package de.syncdroid.service;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.google.inject.Inject;

import de.syncdroid.GuiceService;
import de.syncdroid.SyncBroadcastReceiver;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.work.OneWayCopyJob;

public class SyncService extends GuiceService {
	private static final String TAG = "SyncService";
	private static final int POLL_INTERVALL = 3000;

	public static final String TIMER_TICK = "de.syncdroid.TIMER_TICK";
	public static final String INTENT_START_TIMER = "de.syncdroid.INTENT_START_TIMER";

	@Inject
	private ProfileService profileService;
	
	@Inject
	private LocationService locationService;

	private Thread thread = null;

	public static final int PROFILE_STATUS_UPDATED = 3;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.i(TAG, "onStart()");
		// handle intents
		if (intent != null && intent.getAction() != null) {
			if (intent.getAction().equals(TIMER_TICK)) {
				Log.d(TAG, "TIMER_TICK");
				if (thread != null && thread.isAlive()) {
					Log.w(TAG, "WARNING: TIMER_TICKET while running syncIt");
				} else {
					syncIt();
				}
			} else if (intent.getAction().equals(INTENT_START_TIMER)
					|| intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
				Log.d(TAG, "set timer");
				AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
				Intent i = new Intent(this, SyncBroadcastReceiver.class);
				i.setAction(TIMER_TICK);

				// get a Calendar object with current time
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.SECOND, 4);

				PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
				mgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime(), POLL_INTERVALL, pi);

			}
		} else {
			Log.d(TAG, "unknown intent:");
			Log.d(TAG, "Receive intent= " + intent);
			Log.d(TAG, "action= " + intent.getAction());
		}
	}

	private void syncIt() {
		Log.d(TAG, "syncIt()");

		if(profileService != null) {
			Runnable job = new Runnable() {
				public void run() {
					Looper.prepare();
					for(Profile profile : profileService.list()) {
                        new OneWayCopyJob(
                                SyncService.this, profile,
                                profileService, locationService).run();
					}
				}
			};
				  
			thread = new Thread(job);
			thread.start();
		} else {
			Log.e(TAG, "profileService is NULL");
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
