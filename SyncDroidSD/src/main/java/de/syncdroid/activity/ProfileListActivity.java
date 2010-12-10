package de.syncdroid.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.*;
import de.syncdroid.db.model.ProfileStatusLog;
import de.syncdroid.db.model.enums.ProfileStatusLevel;
import de.syncdroid.db.service.ProfileStatusLogService;
import roboguice.inject.InjectView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.inject.Inject;

import de.syncdroid.AbstractActivity;
import de.syncdroid.R;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.service.SyncService;
import de.syncdroid.work.OneWayCopyJob;

public class ProfileListActivity extends AbstractActivity {
	static final String TAG = "SyncDroid.ProfileListActivity";
	
	@Inject private ProfileService profileService;
	@Inject private ProfileStatusLogService profileStatusLogService;

	@InjectView(R.id.ListView01)             ListView lstProfiles;
	@InjectView(R.id.txtProfileCount)		 TextView txtProfilesCount;
	@InjectView(R.id.btnForceCheck)          Button btnForceCheck;

	private Profile currentlyLongClickedProfile = null;

    private List<Profile> profiles = new ArrayList<Profile>();

	private Map<Long, ProfileStatusLog> profileStatusById = new HashMap<Long, ProfileStatusLog>();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_list_activity);
        
		Intent myIntent = new Intent(this, SyncService.class);
		myIntent.setAction(SyncService.ACTION_START_TIMER);
		startService(myIntent);
		updateProfileList();        

		
		lstProfiles.setOnItemClickListener(
				new AdapterView.OnItemClickListener() {
			 public void onItemClick(AdapterView<?> a, View v, 
					 int position, long id) {
				Profile profile = (Profile)lstProfiles.getItemAtPosition(position);
				Log.d(TAG, "ProfileId: " + profile.getId());
				
				Intent myIntent = new Intent(ProfileListActivity.this, 
						ProfileEditActivity.class);
				myIntent.putExtra(ProfileEditActivity.EXTRA_ID, profile.getId());
				myIntent.setAction(Intent.ACTION_EDIT);
				ProfileListActivity.this.startActivity(myIntent);
			 }});

		lstProfiles.setOnItemLongClickListener(
				new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> a, View v, 
					int position, long id) {
		        Log.i(TAG, "OnItemLongClickListener()");
		        
		        currentlyLongClickedProfile = (Profile)
		        	lstProfiles.getItemAtPosition(position);
				Log.d(TAG, "OnItemLongClickListener(), ProfileId: " + 
						currentlyLongClickedProfile.getId());
				return false;
			}
        });

        btnForceCheck.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent myIntent = new Intent(ProfileListActivity.this, SyncService.class);
                myIntent.setAction(SyncService.ACTION_TIMER_TICK);
                startService(myIntent);
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

        
        IntentFilter filter = new IntentFilter();
        filter.addAction(OneWayCopyJob.ACTION_PROFILE_UPDATE);
		this.registerReceiver(this.receiver, filter);

		updateProfileList();

        for(Profile profile : profiles) {
            ProfileStatusLog statusLog =
                    profileStatusLogService.findLatestByProfile(profile);

            profileStatusById.clear();
            profileStatusById.put(profile.getId(), statusLog);
        }
	}
	
	private void updateProfileList() {
		profiles = profileService.list();
		
		ProfileListAdapter adapter = new ProfileListAdapter(this, 
                R.layout.profile_listitem, R.id.TextView01, 
                profiles.toArray(new Profile[]{}));
        lstProfiles.setAdapter(adapter);        
        txtProfilesCount.setText(String.valueOf(profiles.size()));
	}

    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        this.unregisterReceiver(this.receiver);
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

        Intent intent;

		switch (item.getItemId()) {
		case R.id.item01:
			intent = new Intent(this, LocationListActivity.class);
			startActivity(intent);
			break;
        case R.id.item02:
            intent = new Intent(this, LogViewActivity.class);
            startActivity(intent);
            break;
        case R.id.item03:
            intent = new Intent(this, SettingsActivity.class);
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
			
			View row = inflater.inflate(R.layout.profile_listitem, null);
			Profile p = mItems[position];
			TextView label=(TextView)row.findViewById(R.id.txtProfileName);
			
			if(p.getName() != null) {
				label.setText(p.getName());
			} else {
				label.setText("[empty label]");
			}

			final String UNKNOWN = "unknown";

            ProfileStatusLog profileStatus = null;
			if(profileStatusById.get(p.getId()) != null) {
                profileStatus = profileStatusById.get(p.getId());
			}

			String text = UNKNOWN;
			String detailMessage = "";
            ProfileStatusLevel level = ProfileStatusLevel.INFO;

            if(profileStatus != null) {
                level = profileStatus.getStatusLevel();
                text = profileStatus.getShortMessage();
                detailMessage = profileStatus.getDetailMessage();
            }

            label = (TextView) row.findViewById(R.id.txtProfileStatus);
            label.setText(text);

            switch (level) {
                case INFO:
                    label.setTextColor(mContext.getResources().getColor(R.color.white));
                    break;
                case WARN:
                    label.setTextColor(mContext.getResources().getColor(R.color.yellow));
                    break;
                case ERROR:
                    label.setTextColor(mContext.getResources().getColor(R.color.red));
                    break;
                case SUCCESS:
                    label.setTextColor(mContext.getResources().getColor(R.color.green));
                    break;
            }

			label = (TextView) row.findViewById(R.id.txtProfileStatusDetail);
			label.setText(detailMessage);

			return row;
		}
	}

    
    private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
        	ProfileStatusLog log = (ProfileStatusLog) intent.getSerializableExtra(EXTRA_PROFILE_UPDATE);
            Long id = log.getProfile().getId();

            profileStatusById.put(id, log);
        	lstProfiles.invalidateViews();
		}
     };
}