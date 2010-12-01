package de.syncdroid.activity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.syncdroid.ProfileStatusLevel;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.inject.Inject;

import de.syncdroid.AbstractActivity;
import de.syncdroid.R;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.service.SyncService;
import de.syncdroid.work.OneWayCopyJob;

public class LogViewActivity extends AbstractActivity {
	static final String TAG = "ProfileListActivity";
	
	@InjectView(R.id.ListView01)             ListView lstLogMessage;
	@InjectView(R.id.txtProfileName)		 TextView txtProfilesName;
	
	@Inject private ProfileService profileService;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logview_activity);

        updateLogMessageList();        
    }
	

	@Override
	protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(OneWayCopyJob.ACTION_PROFILE_UPDATE);
		this.registerReceiver(this.receiver, filter);

		updateLogMessageList();
	}
	
	private void updateLogMessageList() {
		/*ProfileListAdapter adapter = new ProfileListAdapter(this, 
                R.layout.profile_listitem, R.id.TextView01, 
                profiles.toArray(new Profile[]{}));
        lstProfiles.setAdapter(adapter);        
        txtProfilesCount.setText(String.valueOf(profiles.size()));*/
	}

    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        this.unregisterReceiver(this.receiver);
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
			
			View row = inflater.inflate(R.layout.profile_listitem, null);
			Profile p = mItems[position];
			TextView label=(TextView)row.findViewById(R.id.txtProfileName);
			
			if(p.getName() != null) {
				label.setText(p.getName());
			} else {
				label.setText("[empty label]");
			}

			final String UNKNOWN = "unknown";

            /*
			ProfileStatus profileStatus = null;
			if(profileStatusById.get(p.getId()) != null) {
                profileStatus = profileStatusById.get(p.getId());
			}
			*/

			String text = UNKNOWN;
			String detailMessage = "";
            ProfileStatusLevel level = ProfileStatusLevel.INFO;

            /*
            if(profileStatus != null) {
                level = profileStatus.level;
                text = profileStatus.message;
                detailMessage = profileStatus.detailMessage;
            }
            */

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
			Long id = intent.getExtras().getLong(EXTRA_ID);
        	String msg = intent.getExtras().getString(EXTRA_MESSAGE);
            String detailMsg = intent.getExtras().getString(EXTRA_DETAILMESSAGE);
            ProfileStatusLevel level = ProfileStatusLevel.valueOf(intent.getExtras().getString(EXTRA_LEVEL));
			Log.d(TAG, "onReceive() : " + msg);
			/*
        	profileStatusById.put(id, new ProfileStatus(msg, detailMsg, level));
        	//lstProfiles.invalidate();
        	lstProfiles.invalidateViews();
        	*/
		}
 
     };


}