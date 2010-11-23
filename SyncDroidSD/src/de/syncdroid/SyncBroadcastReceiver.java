package de.syncdroid;

import de.syncdroid.service.LocationDiscoveryService;
import de.syncdroid.service.SyncService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SyncBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = "SyncBroadcastReceiver";

	public void onReceive(Context context, Intent intent ) {
		Log.d(TAG, "Receive intent= " + intent );
		Log.d(TAG, "action= " + intent.getAction() );
		
		Class<?>[] services = {
				SyncService.class, 
				LocationDiscoveryService.class
		};
		
		for(Class<?> clazz : services) {
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
