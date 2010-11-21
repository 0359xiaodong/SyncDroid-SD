package de.syncdroid;

import java.util.List;

import roboguice.activity.GuiceActivity;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.inject.Inject;

import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.service.SyncService;

public class ProfileListActivity extends GuiceActivity {
	static final String TAG = "ProfileListActivity";
	
	@Inject private ProfileService profileService;

	@InjectView(R.id.ListView01)             ListView lstProfiles;
	
	private Profile currentlyLongClickedProfile = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_list);
        
		Intent myIntent = new Intent(this, SyncService.class);
		myIntent.setAction(SyncService.INTENT_START_TIMER);
		startService(myIntent);
		
		dumpProfiles();
		updateProfileList();
		
		lstProfiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			 public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				Profile profile = (Profile)lstProfiles.getItemAtPosition(position);
				Log.d(TAG, "ProfileId: " + profile.getId());
				
				Intent myIntent = new Intent(ProfileListActivity.this, ProfileEditActivity.class);
				myIntent.putExtra(ProfileEditActivity.PARAM_ID, profile.getId());
				myIntent.putExtra(ProfileEditActivity.PARAM_ACTION, "edit");
				ProfileListActivity.this.startActivity(myIntent);
			 }});

		lstProfiles.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> a, View v, int position, long id) {
		        Log.i(TAG, "OnItemLongClickListener()");
		        currentlyLongClickedProfile = (Profile)
		        	lstProfiles.getItemAtPosition(position);
				Log.d(TAG, "OnItemLongClickListener(), ProfileId: " + 
						currentlyLongClickedProfile.getId());
				return false;
			}
        });
        
        registerForContextMenu(lstProfiles);
        lstProfiles.setOnCreateContextMenuListener(this);

		
    }
	

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
		Log.d(TAG, "onCreateContextMenu(");
    	super.onCreateContextMenu(menu, v, menuInfo);

    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.profile_longclick_menu, menu);
    }
    

	public boolean onContextItemSelected (MenuItem item) {
		super.onContextItemSelected(item);
		Log.d(TAG, "onContextItemSelected()");
		Long id = currentlyLongClickedProfile.getId();
		Log.d(TAG, "PersonId: " + id);

		switch (item.getItemId()) {
		// We have only one menu option
		case R.id.item01: {
			Intent myIntent = new Intent(this, ProfileEditActivity.class);
			myIntent.putExtra(ProfileEditActivity.PARAM_ACTION, ProfileEditActivity.ACTION_EDIT);
			myIntent.putExtra(ProfileEditActivity.PARAM_ID, id);
			startActivity(myIntent);
			break;
		}
			
		case R.id.item02: {
			profileService.delete(currentlyLongClickedProfile);
			updateProfileList();
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
        
		updateProfileList();
	}
	
	private void updateProfileList() {
		List<Profile> profiles = profileService.list();
		
        ListAdapter adapter = new ArrayAdapter<Profile>(this, 
                android.R.layout.simple_list_item_1, 
                profiles.toArray(new Profile[]{}));
        lstProfiles.setAdapter(adapter);                
	}

    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        dumpProfiles();
    }
    
	private void dumpProfiles() {
		List<Profile> profiles = profileService.list();
		
		Log.i(TAG, "-------- Profile DUMP ---------");
		for(Profile profile : profiles) {
			Log.i(TAG, "profile #" + profile.getId() + ": " 
					+ profile.getName());
		}
		Log.i(TAG, "-------------------------------");
	}
	
	public void onButtonAddProfileClick(View view) {
        Log.d(TAG, "onButtonSyncItClick()");
        
		Intent intent = new Intent(this, ProfileEditActivity.class);
		intent.putExtra(ProfileEditActivity.PARAM_ACTION, 
				ProfileEditActivity.ACTION_CREATE);
		startActivity(intent);  
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu()");
		MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.profile_list_menu, menu);
    	return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	Log.d(TAG, "onOptionsItemSelected()");
		switch (item.getItemId()) {
		// We have only one menu option
		case R.id.item01:
			Intent intent = new Intent(this, LocationActivity.class);
			startActivity(intent);
			break;
			
		default:
			Log.d(TAG, "unknown menu");
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

}