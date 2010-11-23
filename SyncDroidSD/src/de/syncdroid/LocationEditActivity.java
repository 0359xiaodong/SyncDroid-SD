package de.syncdroid;

import roboguice.inject.InjectView;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.inject.Inject;

import de.syncdroid.db.model.Location;
import de.syncdroid.db.model.LocationCell;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.service.LocationDiscoveryService;
import de.syncdroid.service.SyncService;


public class LocationEditActivity extends MessageReceiverActivity {
	static final String TAG = "LocationActivity";


	@InjectView(R.id.ListView01) private ListView lv1;
	@InjectView(R.id.EditText01) 			 EditText txtName;
	@Inject private LocationService locationService;
	private Location location;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_edit_activity);
        

        Bundle bundle = getIntent().getExtras();
        
        if(getIntent().getAction().equals(Intent.ACTION_EDIT)) {
        	Long id = bundle.getLong(EXTRA_ID);
        	location = locationService.findById(id);
        	
        	if(location == null) {
        		throw new RuntimeException(
        				"profile with id '" + id + "' not found");
        	}
        } else if(getIntent().getAction().equals(Intent.ACTION_INSERT)) {
        	location = new Location();
        } else {
        	throw new RuntimeException("no action given to Activity");
        }
        
		Intent myIntent = new Intent(this, LocationDiscoveryService.class);
		myIntent.setAction(LocationDiscoveryService.INTENT_COLLECT_CELL_IDS);
		bindService(myIntent, mConnection, 0);
		
		readFromDatabase();
		
    }
	
	private void readFromDatabase() {
        txtName.setText(location.getName());
	}

	private void writeToDatabase() {
		if(txtName.getText().equals("")) {
			return;
		}
		location.setName(txtName.getText().toString());
		
		locationService.saveOrUpdate(location);
	}
	
	public void onSaveClick(View view) {
		writeToDatabase();
		finish();
	}
	

	private void addItem(LocationCell item) {
		location.getLocationCells().add(0, item);
		ListAdapter adapter = new ArrayAdapter<LocationCell>(this, 
				android.R.layout.simple_list_item_1, 
				location.getLocationCells().toArray(new LocationCell[]{}));
		lv1.setAdapter(adapter);
		lv1.refreshDrawableState();
	}

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case LocationDiscoveryService.FOUND_NEW_CELL:
//	                mCallbackText.setText("Received from service: " + msg.arg1);
            	GsmCellLocation cellLocation = (GsmCellLocation)msg.obj;
            	Log.d(TAG, "msg.arg1: " + msg.arg1);
            	Log.d(TAG, "msg.obj: " + cellLocation);
            	
            	LocationCell cell = new LocationCell();
            	cell.setCid(cellLocation.getCid());
            	cell.setLac(cellLocation.getLac());
            	
            	if(!location.getLocationCells().contains(cell)) {
            		addItem(cell);
            	}
            	
            	return true;
            default:
            	return false;
        }
    }

}
