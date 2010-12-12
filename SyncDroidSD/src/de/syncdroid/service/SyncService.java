package de.syncdroid.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.inject.Inject;

import de.syncdroid.AbstractActivity;
import de.syncdroid.R;
import de.syncdroid.SyncBroadcastReceiver;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.model.ProfileStatusLog;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.db.service.ProfileStatusLogService;
import de.syncdroid.work.OneWayCopyJob;

/**
 * Service to handle Account sync. This is invoked with an intent with action
 * ACTION_AUTHENTICATOR_INTENT. It instantiates the syncadapter and returns its
 * IBinder.
 */
public class SyncService extends Service {
	private static final String TAG = "SyncDroid.SyncService";

	public static final String ACTION_TIMER_TICK  
			= "de.syncdroid.ACTION_TIMER_TICK";
	
	public static final String ACTION_START_TIMER 
			= "de.syncdroid.ACTION_START_TIMER";

    Integer currentPollingInterval = null;
    PendingIntent alarmTimerPendingIntent = null;
    Boolean timerEnabled = true;


    private static final Object sSyncAdapterLock = new Object();
    private static SyncAdapter sSyncAdapter = null;
    
    
    @Inject LocationService locationService;
    @Inject ProfileService profileService;

    /*
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true, 
                		profileService, locationService);
            }
        }
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
    

}
