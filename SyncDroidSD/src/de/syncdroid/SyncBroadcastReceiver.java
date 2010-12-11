package de.syncdroid;

import de.syncdroid.service.LocationDiscoveryService;
import de.syncdroid.service.SyncService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SyncBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "SyncDroid.SyncBroadcastReceiver";
	
	private static final Class<?>[] servicesToNotify = {
		SyncService.class, 
		LocationDiscoveryService.class
	};

	public void onReceive(Context context, Intent intent ) {
		if( intent != null && intent.getAction() != null ) {
			Log.d(TAG, "Received intent= " + intent + 
					" with action '" + intent.getAction() + "'");
			
			for(Class<?> clazz : servicesToNotify) {
				Intent serviceIntent = new Intent(context, clazz);
				if( intent.getAction() != null ) {
					serviceIntent.setAction(intent.getAction());
				}		
				if( intent.getExtras() != null ) {
					serviceIntent.putExtras(intent.getExtras());
				}
				context.startService(serviceIntent);
			}
		}
	}
}
