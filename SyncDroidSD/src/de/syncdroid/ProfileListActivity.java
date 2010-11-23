package de.syncdroid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboguice.activity.GuiceActivity;
import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.inject.Inject;

import de.syncdroid.db.model.LocationCell;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.service.LocationDiscoveryService;
import de.syncdroid.service.SyncService;

public class ProfileListActivity extends MessageReceiverActivity {
	static final String TAG = "ProfileListActivity";
	
	@Inject private ProfileService profileService;
	@InjectView(R.id.ListView01)             ListView lstProfiles;
	@InjectView(R.id.txtProfileCount)		 TextView txtProfilesCount;
	
	private Profile currentlyLongClickedProfile = null;
	
	private Map<Long, String> profileStatusById = new HashMap<Long, String>();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_list_activity);
        
		Intent myIntent = new Intent(this, SyncService.class);
		myIntent.setAction(SyncService.INTENT_START_TIMER);
		bindService(myIntent, mConnection, 0);
		
		dumpProfiles();
		updateProfileList();
		
		lstProfiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			 public void onItemClick(AdapterView<?> a, View v, int position, long id) {
				Profile profile = (Profile)lstProfiles.getItemAtPosition(position);
				Log.d(TAG, "ProfileId: " + profile.getId());
				
				Intent myIntent = new Intent(ProfileListActivity.this, ProfileEditActivity.class);
				myIntent.putExtra(ProfileEditActivity.EXTRA_ID, profile.getId());
				myIntent.setAction(Intent.ACTION_EDIT);
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
			myIntent.setAction(Intent.ACTION_EDIT);
			myIntent.putExtra(ProfileEditActivity.EXTRA_ID, id);
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
		
		ProfileListAdapter adapter = new ProfileListAdapter(this, 
                R.layout.profile_listitem, R.id.TextView01, 
                profiles.toArray(new Profile[]{}));
        lstProfiles.setAdapter(adapter);        
        txtProfilesCount.setText(String.valueOf(profiles.size()));
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
		intent.setAction(Intent.ACTION_INSERT);
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
			Intent intent = new Intent(this, LocationListActivity.class);
			startActivity(intent);
			break;
			
		default:
			Log.d(TAG, "unknown menu");
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	

	public class ProfileListAdapter extends ArrayAdapter<Profile> {
		private Context mContext;
		private Profile[] mItems;
		
		
		public ProfileListAdapter(Context context, int resource,
				int textViewResourceId, Profile[] objects) {
			
			super(context, resource, textViewResourceId, objects);
			mContext = context;
			mItems = objects;
		}

		public View getView(int position, View ConvertView, ViewGroup parent) {
			Log.d(TAG, "getView(" + position + ")");
			LayoutInflater inflater = (LayoutInflater)
				mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if (inflater == null) {
				Log.e(TAG, "inflater is null!");
			}
			
			View row=inflater.inflate(R.layout.profile_listitem, null);
			Profile p = mItems[position];
			TextView label=(TextView)row.findViewById(R.id.txtProfileName);
			label.setText(p.getName());

			final String UNKNOWN = "unknown";
			
			String text = UNKNOWN;
			
			if(profileStatusById.get(p.getId()) != null) {
				text = profileStatusById.get(p.getId());
			}
			
			label=(TextView)row.findViewById(R.id.txtProfileStatus);
			label.setText(text);

			if (UNKNOWN.equals(text)) {
				label.setTextColor(mContext.getResources().getColor(R.color.white));
			} else {
				label.setTextColor(mContext.getResources().getColor(R.color.green));
			}

			return row;
		}
	}


    public boolean handleMessage(Message msg) {
    	Log.i(TAG, "handleMessage() " + msg.what);
        switch (msg.what) {
            case SyncService.PROFILE_STATUS_UPDATED:
//	                mCallbackText.setText("Received from service: " + msg.arg1);
            	
            	ProfileHelper profileHelper = (ProfileHelper) msg.obj;
            	profileStatusById.put(profileHelper.id, profileHelper.message);
            	//lstProfiles.invalidate();
            	lstProfiles.invalidateViews();
            	return true;
            default:
            	return false;
        }
    }

}