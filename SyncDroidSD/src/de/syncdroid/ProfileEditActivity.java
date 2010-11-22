package de.syncdroid;

import roboguice.activity.GuiceActivity;
import roboguice.inject.InjectView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.inject.Inject;

import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.ProfileService;

public class ProfileEditActivity extends AbstractActivity  {
	static final String TAG = "ProfileActivity";

	
	private Profile profile;
	
	@InjectView(R.id.EditText01)             EditText txtLocalDirectory;
	@InjectView(R.id.EditText02)             EditText txtFtpHost;
	@InjectView(R.id.EditText03)             EditText txtFtpUsername;
	@InjectView(R.id.EditText04)             EditText txtFtpPassword;
	@InjectView(R.id.EditText05)             EditText txtFtpPath;
	@InjectView(R.id.EditText06)             EditText txtProfileName;
	
    @Inject                            		 ProfileService profileService; 
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_edit);

        Bundle bundle = getIntent().getExtras();
        
        if(getIntent().getAction().equals(Intent.ACTION_EDIT)) {
        	Long id = bundle.getLong(EXTRA_ID);
        	profile = profileService.findById(id);
        	
        	if(profile == null) {
        		throw new RuntimeException(
        				"profile with id '" + id + "' not found");
        	}
        } else if(getIntent().getAction().equals(Intent.ACTION_INSERT)) {
        	profile = new Profile();
        } else {
        	throw new RuntimeException("no action given to Activity");
        }
        
        readFromDatabase();
    }

	private void readFromDatabase() {
        txtProfileName.setText(profile.getName());
        txtLocalDirectory.setText(profile.getLocalPath());
        txtFtpHost.setText(profile.getHostname());
        txtFtpUsername.setText(profile.getUsername());
        txtFtpPassword.setText(profile.getPassword());
        txtFtpPath.setText(profile.getRemotePath());
	}

	private void writeToDatabase() {
		profile.setName(txtProfileName.getText().toString());
		profile.setLocalPath(txtLocalDirectory.getText().toString());
		profile.setHostname(txtFtpHost.getText().toString());
		profile.setUsername(txtFtpUsername.getText().toString());
		profile.setPassword(txtFtpPassword.getText().toString());
		profile.setRemotePath(txtFtpPath.getText().toString());
		
		profileService.saveOrUpdate(profile);
	}
    protected void onPause() {
        Log.i(TAG, "onPause()");
        super.onPause();

    	writeToDatabase();
    }

    
	public void onButtonSyncItClick(View view) {
        Log.i(TAG, "onButtonSyncItClick()");
        writeToDatabase();
        
        finish();
	}
}