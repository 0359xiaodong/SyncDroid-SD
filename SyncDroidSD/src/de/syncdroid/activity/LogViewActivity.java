package de.syncdroid.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.*;
import android.widget.Button;
import de.syncdroid.db.model.ProfileStatusLog;
import de.syncdroid.db.model.enums.ProfileStatusLevel;
import roboguice.inject.InjectView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.inject.Inject;

import de.syncdroid.AbstractActivity;
import de.syncdroid.R;
import de.syncdroid.db.service.ProfileStatusLogService;
import de.syncdroid.work.OneWayCopyJob;

public class LogViewActivity extends AbstractActivity {
	static final String TAG = "SyncDroid.ProfileListActivity";
	
	@InjectView(R.id.ListView01)             ListView lstLogMessage;
	@InjectView(R.id.txtProfileName)		 TextView txtProfilesName;
	@InjectView(R.id.btnClearLogs)
    Button btnClearLogs;

	@Inject private ProfileStatusLogService profileStatusLogService;

    private String lastShortMessage = null;
    private String lastDetailMessage = null;

	private List<ProfileStatusLog> profileStatusLogs = 
		new ArrayList<ProfileStatusLog>();

	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logview_activity);

        btnClearLogs.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                profileStatusLogService.deleteAll();
                profileStatusLogs.clear();
                updateLogMessageList();
            }
        });
        
        profileStatusLogs.addAll(profileStatusLogService.list());
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
		ProfileStatusLogListAdapter adapter = new ProfileStatusLogListAdapter(this,
                R.layout.profile_listitem, R.id.TextView01, 
                profileStatusLogs.toArray(new ProfileStatusLog[]{}));
        lstLogMessage.setAdapter(adapter);
        txtProfilesName.setText(String.valueOf(profileStatusLogs.size()));
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
	

	public class ProfileStatusLogListAdapter extends ArrayAdapter<ProfileStatusLog> {
		private Context mContext;
		private ProfileStatusLog[] mItems;
		
		
		public ProfileStatusLogListAdapter(Context context, int resource,
				int textViewResourceId, ProfileStatusLog[] objects) {
			
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
			ProfileStatusLog p = mItems[position];
			TextView label=(TextView)row.findViewById(R.id.txtProfileName);


            SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            label.setText(dateFormat.format(p.getTimestamp()));

			String shortMessage = p.getShortMessage();
			String detailMessage = p.getDetailMessage();
            ProfileStatusLevel level = p.getStatusLevel();

			label = (TextView) row.findViewById(R.id.txtProfileStatus);
			label.setText(shortMessage);

            if(level != null) {
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

            if(log.getDetailMessage().equals(lastDetailMessage)
                    && log.getShortMessage().equals(lastShortMessage)) {

                Log.d(TAG, "ignoring duplicate message");
            } else {
                Log.d(TAG, "onReceive() : " + log.getShortMessage() + " [" + log.getDetailMessage() + "] @ " +
                        log.getTimestamp());

                profileStatusLogs.add(log);

                Collections.sort(profileStatusLogs, new Comparator<ProfileStatusLog>() {
                    public int compare(ProfileStatusLog o1, ProfileStatusLog o2) {
                        return (int) (o2.getTimestamp().getTime() - o1.getTimestamp().getTime());
                    }
                });

                updateLogMessageList();
            }
		}
 
     };


}