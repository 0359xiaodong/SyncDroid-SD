package de.syncdroid.activity;

import java.util.List;

import roboguice.inject.InjectView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.inject.Inject;

import de.syncdroid.AbstractActivity;
import de.syncdroid.R;
import de.syncdroid.db.model.Location;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.service.SyncService;

public class LocationListActivity extends AbstractActivity {
	static final String TAG = "LocationListActivity";
	
	@Inject private LocationService locationService;
	@InjectView(R.id.ListView01)             ListView lstLocations;
	
	private Location currentlyLongClickedLocation = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_list_activity);
        
		Intent myIntent = new Intent(this, SyncService.class);
		myIntent.setAction(SyncService.INTENT_START_TIMER);
		startService(myIntent);
		
		dumpLocations();
		updateLocationList();
		
		lstLocations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			 public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				Location location = (Location)lstLocations.getItemAtPosition(position);
				Log.d(TAG, "LocationId: " + location.getId());
				
				Intent myIntent = new Intent(LocationListActivity.this, LocationEditActivity.class);
				myIntent.putExtra(LocationEditActivity.EXTRA_ID, location.getId());
				myIntent.setAction(Intent.ACTION_EDIT);
				LocationListActivity.this.startActivity(myIntent);
			 }});

		lstLocations.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> a, View v, int position, long id) {
		        Log.i(TAG, "OnItemLongClickListener()");
		        currentlyLongClickedLocation = (Location)
		        	lstLocations.getItemAtPosition(position);
				Log.d(TAG, "OnItemLongClickListener(), LocationId: " + 
						currentlyLongClickedLocation.getId());
				return false;
			}
        });
        
        registerForContextMenu(lstLocations);
        lstLocations.setOnCreateContextMenuListener(this);

		
    }
	

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
		Log.d(TAG, "onCreateContextMenu(");
    	super.onCreateContextMenu(menu, v, menuInfo);

    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.location_longclick_menu, menu);
    }
    

	public boolean onContextItemSelected (MenuItem item) {
		super.onContextItemSelected(item);
		Log.d(TAG, "onContextItemSelected()");
		Long id = currentlyLongClickedLocation.getId();
		Log.d(TAG, "PersonId: " + id);

		switch (item.getItemId()) {
		// We have only one menu option
		case R.id.item01: {
			Intent myIntent = new Intent(this, LocationEditActivity.class);
			myIntent.setAction(Intent.ACTION_EDIT);
			myIntent.putExtra(LocationEditActivity.EXTRA_ID, id);
			startActivity(myIntent);
			break;
		}
			
		case R.id.item02: {
			locationService.delete(currentlyLongClickedLocation);
			updateLocationList();
			break;
		}

		default:
			Log.w(TAG, "unknown menu");
		}
		return true;
	}
	
	@Override
	protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();


		updateLocationList();
	}
	
	private void updateLocationList() {
		List<Location> locations = locationService.list();
		
        ListAdapter adapter = new ArrayAdapter<Location>(this, 
                android.R.layout.simple_list_item_1, 
                locations.toArray(new Location[]{}));
        lstLocations.setAdapter(adapter);
	}

    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();


        dumpLocations();
    }
    
	private void dumpLocations() {
		List<Location> locations = locationService.list();
		
		Log.i(TAG, "-------- Location DUMP ---------");
		for(Location location : locations) {
			Log.i(TAG, "location #" + location.getId() + ": " 
					+ location.getName());
		}
		Log.i(TAG, "-------------------------------");
	}
	
	public void onButtonAddLocationClick(View view) {
        Log.d(TAG, "onButtonSyncItClick()");
        
		Intent intent = new Intent(this, LocationEditActivity.class);
		intent.setAction(Intent.ACTION_INSERT);
		startActivity(intent);  
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	Log.d(TAG, "onOptionsItemSelected()");
		switch (item.getItemId()) {
		// We have only one menu option
		case R.id.item01:
			Intent intent = new Intent(this, LocationListActivity.class);
			startActivity(intent);
			break;
			
		default:
			Log.d(TAG, "unknown menu");
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	


}