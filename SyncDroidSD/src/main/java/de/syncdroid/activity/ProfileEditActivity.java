package de.syncdroid.activity;

import java.io.File;
import java.util.List;

import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.inject.Inject;

import de.syncdroid.AbstractActivity;
import de.syncdroid.R;
import de.syncdroid.db.model.Location;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.model.enums.ProfileType;
import de.syncdroid.db.model.enums.SyncType;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.transfer.impl.DropboxFileTransferClient;

public class ProfileEditActivity extends AbstractActivity  {
	static final String TAG = "SyncDroid.ProfileEditActivity";

	private Profile profile;
	
	@InjectView(R.id.EditText01)             EditText txtLocalPath;
	@InjectView(R.id.EditText02)             EditText txtHostname;
	@InjectView(R.id.EditText03)             EditText txtUsername;
	@InjectView(R.id.EditText04)             EditText txtPassword;
	@InjectView(R.id.EditText05)             EditText txtRemotePath;
	@InjectView(R.id.EditText06)             EditText txtProfileName;
	@InjectView(R.id.EditText07)             EditText txtPort;
	@InjectView(R.id.Spinner01)				 Spinner  spnLocationList;
	@InjectView(R.id.Spinner02)				 Spinner  spnProfileTypeList;
	@InjectView(R.id.Spinner03)				 Spinner  spnSyncTypeList;
	@InjectView(R.id.CheckBox01)		     CheckBox  chkOnlyIfWifi;
	@InjectView(R.id.CheckBox02)		     CheckBox  chkEnabled;
	
	@InjectView(R.id.HostPortWrapper)	 	 LinearLayout hostnamePortWrapper;
	@InjectView(R.id.UsernamePasswordWrapper) LinearLayout usernamePasswordWrapper;
	@InjectView(R.id.PortWrapper)	 		 LinearLayout portWrapper;
	@InjectView(R.id.DropboxWrapper)	     LinearLayout dropboxWrapper;
	@InjectView(R.id.DropboxClearWrapper)	     LinearLayout dropboxClearWrapper;
	
	@InjectView(R.id.AuthenticateWithDropbox) Button btnAuthenticateWithDropbox;
	@InjectView(R.id.ClearDropboxAuth) Button btnClearDropboxAuth;
	@InjectView(R.id.btnOpenDirectory) Button btnOpenDirectory;
	
    @Inject                            		 ProfileService profileService; 
    @Inject                            		 LocationService locationService;

    private static final int PICK_DIRECTORY_RESPONSE = 4711;
	
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
        
        switchToDefaultView();
        
        btnAuthenticateWithDropbox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
        		Log.i(TAG, "on dropbox authenticate button click");
				String username = txtUsername.getText().toString();
				String password = txtPassword.getText().toString();

				DropboxFileTransferClient dropboxFileTransferClient = 
					new DropboxFileTransferClient(ProfileEditActivity.this);
				
				if(dropboxFileTransferClient.
						authenticate(username, password)) {
					Toast.makeText(ProfileEditActivity.this, 
							"dropbox auth successful", 2000).show();

                    txtUsername.setText("");
                    txtPassword.setText("");
				} else {
					Toast.makeText(ProfileEditActivity.this, 
							"dropbox auth failed", 2000).show();
				}
				
				switchToDropboxView();
			}
		});

        btnOpenDirectory.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(TAG, "starting pick directory intent");

                Intent intent;

                String path = "/sdcard/";

                if(txtLocalPath.getText().toString().equals("") == false) {
                    path = txtLocalPath.getText().toString();
                }

                File file = new File(path);

                Uri uri = Uri.fromFile(file);

                intent = new Intent("org.openintents.action.PICK_DIRECTORY");
                intent.setData(uri);
                intent.putExtra("org.openintents.extra.TITLE",
                    "select directory");
                intent.putExtra("org.openintents.extra.BUTTON_TEXT",
                    "select directory");
                try {
                  startActivityForResult(intent, PICK_DIRECTORY_RESPONSE);
                  return;
                } catch(ActivityNotFoundException e) {
                }

                Toast.makeText(ProfileEditActivity.this, "please install 'OI File Manager' for this to work",
                    Toast.LENGTH_SHORT).show();
                return;
			}
		});
        
        btnClearDropboxAuth.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		Log.i(TAG, "on clear dropbox authenticate button click");

                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileEditActivity.this);
                builder.setMessage(getResources().getString(R.string.clear_dropbox_auth) + "?");
                builder.setPositiveButton(R.string.clear_dropbox_auth, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DropboxFileTransferClient dropboxFileTransferClient =
                            new DropboxFileTransferClient(ProfileEditActivity.this);

                        dropboxFileTransferClient.clearKeys();
                        switchToDropboxView();
                    }
                });

                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
        	}
        });
        
        spnProfileTypeList.setOnItemSelectedListener(new OnItemSelectedListener() {
        	@Override
        	public void onItemSelected(AdapterView<?> parent, View view,
        			int position, long id) {
        		Log.i(TAG, "onProfileSelected");
        		ProfileType profileType = (ProfileType)
                spnProfileTypeList.getSelectedItem();

				switchToDefaultView();
        		
        		switch (profileType) {
				case DROPBOX:
					switchToDropboxView();
					break;
				case SMB: 
					switchToSmbView();
					break;			
				case FTP:
					if("".equals(txtPort.getText().toString())
							|| "".equals(txtHostname.getText().toString())) {
						txtPort.setText("21");
					}
					break;
				case SCP:
					if("".equals(txtPort.getText().toString()) ||
							"".equals(txtHostname.getText().toString())) {
						txtPort.setText("22");
					}
					break;
				default:
					break;
				}
        	}
        	
        	@Override
        	public void onNothingSelected(AdapterView<?> arg0) {
        		switchToDefaultView();
        	}
		});        
        
        readFromDatabase();
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		Log.i(TAG, "onActivityResult req " + requestCode + " res " + resultCode);

        switch (requestCode) {
            case PICK_DIRECTORY_RESPONSE:
                if (resultCode == RESULT_OK && data != null) {
                    // obtain the filename
                    String filename = data.getDataString();
                    if (filename != null) {
                            // Get rid of URI prefix:
                            if (filename.startsWith("file://")) {
                                    filename = filename.substring(7);
                            }

                            filename = data.getData().getPath();

                            Log.e(TAG, "path: '" + filename + "'");
                            txtLocalPath.setText(filename);
                    }
                } else {
                    Log.e(TAG, "error in retrieving directory path");
                    Toast.makeText(ProfileEditActivity.this,
                        "error in retrieving directory path", 2000).show();
                }
                break;
        }

		if(resultCode == PICK_DIRECTORY_RESPONSE && data != null) {
			Uri uri = data.getData();
			Log.i(TAG, "uri: " + uri);

            Log.i(TAG, "fragment: " + uri.getFragment());
            Log.i(TAG, "host: " + uri.getHost());
            Log.i(TAG, "query: " + uri.getQuery());
            Log.i(TAG, "path: " + uri.getPath());

            txtLocalPath.setText(uri.getPath());
		}

        else {
            Log.e(TAG, "unknown activity result with data: " + data);
        }
		
	}
	
	private void switchToDefaultView() {
		this.dropboxWrapper.setVisibility(View.GONE);
		this.dropboxClearWrapper.setVisibility(View.GONE);
		this.portWrapper.setVisibility(View.VISIBLE);
		this.hostnamePortWrapper.setVisibility(View.VISIBLE);
		this.usernamePasswordWrapper.setVisibility(View.VISIBLE);
	}
	
	private void switchToDropboxView() {
		Log.i(TAG, "switching to dropbox view");
		this.hostnamePortWrapper.setVisibility(View.GONE);
		
		DropboxFileTransferClient dropboxFileTransferClient = 
			new DropboxFileTransferClient(this);
		
		if(dropboxFileTransferClient.isAuthenticated()) {
			dropboxWrapper.setVisibility(View.GONE);
			dropboxClearWrapper.setVisibility(View.VISIBLE);
			usernamePasswordWrapper.setVisibility(View.GONE);
		} else {
			dropboxWrapper.setVisibility(View.VISIBLE);
			dropboxClearWrapper.setVisibility(View.GONE);
			usernamePasswordWrapper.setVisibility(View.VISIBLE);
		}
	}
	
	private void switchToSmbView() {
		Log.i(TAG, "switching to smb view");
		this.portWrapper.setVisibility(View.GONE);
	}

	private void readFromDatabase() {
		Log.i(TAG, "readFromDatabase");
        // setup text fields
        txtProfileName.setText(profile.getName());
        txtLocalPath.setText(profile.getLocalPath());
        txtHostname.setText(profile.getHostname());
        txtUsername.setText(profile.getUsername());
        txtPassword.setText(profile.getPassword());
        txtRemotePath.setText(profile.getRemotePath());
        txtPort.setText(profile.getPort() != null ? 
        		profile.getPort().toString() : "");
        chkOnlyIfWifi.setChecked(profile.getOnlyIfWifi());
        chkEnabled.setChecked(profile.getEnabled());
        
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

        // setup spnSyncTypeList
        adapter = new ArrayAdapter<SyncType>(this,
        		android.R.layout.simple_spinner_item,
        		SyncType.values());
        adapter.setDropDownViewResource(
        		android.R.layout.simple_spinner_dropdown_item);
        spnSyncTypeList.setAdapter(adapter);

        index = 0;
        if(profile.getSyncType() != null) {
	        for(SyncType syncType : SyncType.values()) {
	        	if(profile.getSyncType().toString().equals(
	        			syncType.toString())) {

	        		spnSyncTypeList.setSelection(index);
	                break;
	        	}
	        	index ++;
	        }
        }
	}

	private void writeToDatabase() {
		Log.i(TAG, "writeToDatabase");
		if(txtProfileName.getText().toString().equals("")) {
			return;
		}
		profile.setName(txtProfileName.getText().toString());
		
		try {
			profile.setPort(Integer.valueOf(txtPort.getText().toString()));
		} catch(Exception e) {};
		
		profile.setName(txtProfileName.getText().toString());
		profile.setLocalPath(txtLocalPath.getText().toString());
		profile.setHostname(txtHostname.getText().toString());
		
		
		
		
		profile.setRemotePath(txtRemotePath.getText().toString());
		profile.setOnlyIfWifi(chkOnlyIfWifi.isChecked());
		profile.setEnabled(chkEnabled.isChecked());
		
		Location location = (Location) spnLocationList.getSelectedItem();
		if(location.getId() != null && location.getId() != 0) {
			profile.setLocation(location);
		} else {
            profile.setLocation(null);
        }
		
        ProfileType profileType = (ProfileType)
            spnProfileTypeList.getSelectedItem();

        profile.setProfileType(profileType);
        

		if(profileType == ProfileType.DROPBOX) {
			profile.setUsername("");
			profile.setPassword("");
		} else {
			profile.setUsername(txtUsername.getText().toString());
			profile.setPassword(txtPassword.getText().toString());
			
		}

        SyncType syncType = (SyncType)
            spnSyncTypeList.getSelectedItem();

        profile.setSyncType(syncType);

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