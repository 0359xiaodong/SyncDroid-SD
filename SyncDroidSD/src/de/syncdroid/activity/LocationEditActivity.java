package de.syncdroid.activity;

import java.util.List;

import roboguice.inject.InjectView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.inject.Inject;

import de.syncdroid.AbstractActivity;
import de.syncdroid.R;
import de.syncdroid.db.model.Location;
import de.syncdroid.db.model.LocationCell;
import de.syncdroid.db.service.LocationCellService;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.service.LocationDiscoveryService;


public class LocationEditActivity extends AbstractActivity {
	static final String TAG = "LocationActivity";

	static public final String EXTRA_CELL_LAC = "lac";
	static public final String EXTRA_CELL_CID = "cid";

	@InjectView(R.id.ListView01) private ListView lv1;
	@InjectView(R.id.EditText01) 			 EditText txtName;
	@Inject private LocationService locationService;
	@Inject private LocationCellService locationCellService;
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
		startService(myIntent);
		
		readFromDatabase();
		
    }
	 
	@Override
	protected void onResume() {
		super.onResume();

        
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationDiscoveryService.ACTION_CELL_CHANGED);
		this.registerReceiver(this.receiver, filter);
	}
	
	private void readFromDatabase() {
        txtName.setText(location.getName());
	}

	private void writeToDatabase() {
		if(txtName.getText().equals("")) {
			return;
		}
		location.setName(txtName.getText().toString());
		
		if(location.getLocationCells() != null) {
			List<LocationCell> locationCells = 
				locationCellService.findAllbyLocation(location);

			// delete missing cells
			for(LocationCell cell : locationCells) {
				if(location.getLocationCells().contains(cell) == false) {
					locationCellService.delete(cell);
				}
			}
			
			// add new cells
			for(LocationCell cell : location.getLocationCells()) {
				if(locationCells.contains(cell) == false) {
					cell.setLocationId(location.getId());
					locationCellService.saveOrUpdate(cell);
				}
			}
		}
		
		
		
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
	

    private BroadcastReceiver receiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	Integer cid = intent.getExtras().getInt(EXTRA_CELL_CID);
	    	Integer lac = intent.getExtras().getInt(EXTRA_CELL_LAC);
        	
        	LocationCell cell = new LocationCell();
        	cell.setCid(cid);
        	cell.setLac(lac);
        	
        	Log.d(TAG, "received cell location: " + cell);
        	
        	if(!location.getLocationCells().contains(cell)) {
        		addItem(cell);
        	}
	    }   
    };

}
