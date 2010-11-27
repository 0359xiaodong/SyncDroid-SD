package de.syncdroid.activity;

import java.util.List;

import roboguice.inject.InjectView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.inject.Inject;

import de.syncdroid.AbstractActivity;
import de.syncdroid.R;
import de.syncdroid.db.model.Location;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.model.ProfileType;
import de.syncdroid.db.service.LocationService;
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
	@InjectView(R.id.Spinner01)				 Spinner  spnLocationList;
	@InjectView(R.id.Spinner02)				 Spinner  spnProfileTypeList;
	@InjectView(R.id.CheckBox01)		     CheckBox  chkOnlyIfWifi;
	
    @Inject                            		 ProfileService profileService; 
    @Inject                            		 LocationService locationService; 
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_edit_activity);

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
        // setup text fields
        txtProfileName.setText(profile.getName());
        txtLocalDirectory.setText(profile.getLocalPath());
        txtFtpHost.setText(profile.getHostname());
        txtFtpUsername.setText(profile.getUsername());
        txtFtpPassword.setText(profile.getPassword());
        txtFtpPath.setText(profile.getRemotePath());
        chkOnlyIfWifi.setChecked(profile.getOnlyIfWifi());
        

        // setup spnLocationList
        List<Location> locations = locationService.list();
        Location anyLocation = new Location();
        anyLocation.setId(0L);
        anyLocation.setName(getResources().getString(R.string.any));
        
        locations.add(0, anyLocation);

        ArrayAdapter<?> adapter = new ArrayAdapter<Location>(this, 
        		android.R.layout.simple_spinner_item, 
        		locations.toArray(new Location[]{}));

        adapter.setDropDownViewResource(
        		android.R.layout.simple_spinner_dropdown_item);
        spnLocationList.setAdapter(adapter);
        
        int index = 0;
        
        if(profile.getLocation() != null) {
	        for(Location location : locations) {
	        	if(location.getId().equals(profile.getLocation().getId())) {
	                spnLocationList.setSelection(index);
	                break;
	        	}
	        	index ++;
	        }
        }

        // setup spnProfileTypeList
        adapter = new ArrayAdapter<ProfileType>(this, 
        		android.R.layout.simple_spinner_item, 
        		ProfileType.values());
        adapter.setDropDownViewResource(
        		android.R.layout.simple_spinner_dropdown_item);
        spnProfileTypeList.setAdapter(adapter);

        index = 0;
        if(profile.getProfileType() != null) {
	        for(ProfileType profileType : ProfileType.values()) {
	        	if(profile.getProfileType().toString().equals(
	        			profileType.toString())) {
	        		
	        		spnProfileTypeList.setSelection(index);
	                break;
	        	}
	        	index ++;
	        }
        }        
	}

	private void writeToDatabase() {
		if(txtProfileName.getText().toString().equals("")) {
			return;
		}
		profile.setName(txtProfileName.getText().toString());
		profile.setLocalPath(txtLocalDirectory.getText().toString());
		profile.setHostname(txtFtpHost.getText().toString());
		profile.setUsername(txtFtpUsername.getText().toString());
		profile.setPassword(txtFtpPassword.getText().toString());
		profile.setRemotePath(txtFtpPath.getText().toString());
		profile.setOnlyIfWifi(chkOnlyIfWifi.isChecked());
		
		Location location = (Location) spnLocationList.getSelectedItem();
		if(location.getId() != 0) {
			profile.setLocation(location);
		}
		
		ProfileType profileType = (ProfileType) 
			spnProfileTypeList.getSelectedItem();
		
		profile.setProfileType(profileType);
		
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