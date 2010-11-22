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
import de.syncdroid.service.SyncService;


public class LocationEditActivity extends AbstractActivity {
	static final String TAG = "LocationActivity";

	
	
	/** Messenger for communicating with service. */
	private Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	private boolean mIsBound;

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
        
		Intent myIntent = new Intent(this, SyncService.class);
		myIntent.setAction(SyncService.INTENT_COLLECT_CELL_IDS);
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
	
    protected void onPause() {
        Log.i(TAG, "onPause()");
        super.onPause();

        try {
            Message msg = Message.obtain(null,
                    SyncService.MSG_UNREGISTER_CLIENT);
            msg.replyTo = mMessenger;
            mService.send(msg);
        } catch (RemoteException e) {
            // In this case the service has crashed before we could even
            // do anything with it; we can count on soon being
            // disconnected (and then reconnected if it can be restarted)
            // so there is no need to do anything here.
        }
        unbindService(mConnection);
}

	private void addItem(LocationCell item) {
		location.getLocationCells().add(0, item);
		ListAdapter adapter = new ArrayAdapter<LocationCell>(this, 
				android.R.layout.simple_list_item_1, 
				location.getLocationCells().toArray(new LocationCell[]{}));
		lv1.setAdapter(adapter);
		lv1.refreshDrawableState();
	}

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
	    @Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	            case SyncService.FOUND_NEW_CELL:
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
	            	
	                break;
	            default:
	                super.handleMessage(msg);
	        }
	    }
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mService = new Messenger(service);
//	        mCallbackText.setText("Attached.");
	        Log.d(TAG, "Attached");

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        try {
	            Message msg = Message.obtain(null,
	                    SyncService.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessenger;
	            mService.send(msg);

//	            // Give it some value as an example.
//	            msg = Message.obtain(null,
//	                    SyncService.MSG_SET_VALUE, this.hashCode(), 0);
//	            mService.sen(msg);
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }

	        // As part of the sample, tell the user what happened.
	        Toast.makeText(LocationEditActivity.this, R.string.remote_service_connected,
	                Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        //mCallbackText.setText("Disconnected.");

	        // As part of the sample, tell the user what happened.
	        Toast.makeText(LocationEditActivity.this, R.string.remote_service_disconnected,
	                Toast.LENGTH_SHORT).show();
	    }
	};

	void doBindService() {
	    // Establish a connection with the service.  We use an explicit
	    // class name because there is no reason to be able to let other
	    // applications replace our component.
	    bindService(new Intent(LocationEditActivity.this, 
	            SyncService.class), mConnection, Context.BIND_AUTO_CREATE);
	    mIsBound = true;
	   // mCallbackText.setText("Binding.");
	}

	void doUnbindService() {
	    if (mIsBound) {
	        // If we have received the service, and hence registered with
	        // it, then now is the time to unregister.
	        if (mService != null) {
	            try {
	                Message msg = Message.obtain(null,
	                		SyncService.MSG_UNREGISTER_CLIENT);
	                msg.replyTo = mMessenger;
	                mService.send(msg);
	            } catch (RemoteException e) {
	                // There is nothing special we need to do if the service
	                // has crashed.
	            }
	        }

	        // Detach our existing connection.
	        unbindService(mConnection);
	        mIsBound = false;
	        //mCallbackText.setText("Unbinding.");
	    }
	}
}
