package de.syncdroid.db.service.impl;

import java.text.ParseException;

import com.google.inject.Inject;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import de.syncdroid.db.model.Profile;
import de.syncdroid.db.model.enums.ProfileType;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;

public class ProfileServiceImpl extends AbstractServiceImpl<Profile> implements ProfileService {
	private static final String TAG = "SyncDroid.ProfileServiceImpl";
	@Inject private LocationService locationService;
	
	protected String getTableName() {
		return "profiles";
	}
	
	protected Profile read(Cursor cursor) {
		Profile obj = new Profile();
		
		if(cursor.getCount() == 0) {
			return null;
		}
		
		obj.setName(cursor.getString(cursor.getColumnIndex("name")));
		obj.setId(cursor.getLong(cursor.getColumnIndex("id")));

		String dateString =
			cursor.getString(cursor.getColumnIndex("lastSync"));
		try {
			if(dateString != null && "".equals(dateString) == false) {
				obj.setLastSync(dateFormat.parse(dateString));
			}
		} catch (ParseException e) {
			Log.e(TAG, "parseException for: " + dateString);
		}
		
		obj.setOnlyIfWifi(cursor.getInt(cursor.getColumnIndex("onlyIfWifi")) == 1);
		obj.setEnabled(cursor.getInt(cursor.getColumnIndex("enabled")) == 1);
		obj.setHostname(cursor.getString(cursor.getColumnIndex("hostname")));
		obj.setUsername(cursor.getString(cursor.getColumnIndex("username")));
		obj.setPassword(cursor.getString(cursor.getColumnIndex("password")));
		obj.setLocalPath(cursor.getString(cursor.getColumnIndex("localPath")));
		obj.setRemotePath(cursor.getString(cursor.getColumnIndex("remotePath")));
		
		obj.setProfileType(ProfileType.getByCode(
                cursor.getString(cursor.getColumnIndex("profile_type"))));
		obj.setPort(cursor.getInt(cursor.getColumnIndex("port")));
		

		Long locationId = cursor.getLong(cursor.getColumnIndex("location_id"));
 		
		if(locationId != null && locationId != 0) {
			obj.setLocation(locationService.findById(locationId));
		}
		
		return obj;
	}
	
	protected ContentValues write(Profile obj) {
		ContentValues values = new ContentValues();
		values.put("id", obj.getId());
		values.put("name", obj.getName());
		values.put("onlyIfWifi", (obj.getOnlyIfWifi() != null 
				? obj.getOnlyIfWifi() : false) ? 1 : 0);
		values.put("enabled", (obj.getEnabled() != null 
				? obj.getEnabled() : false) ? 1 : 0);

		values.put("hostname", obj.getHostname());
		values.put("username", obj.getUsername());
		values.put("password", obj.getPassword());
		values.put("localPath", obj.getLocalPath());
		values.put("remotePath", obj.getRemotePath());
		values.put("profile_type", obj.getProfileType().toString());
		
		values.put("lastSync", obj.getLastSync() == null ? null : 			
			dateFormat.format(obj.getLastSync()));
		
		values.put("port", obj.getPort());
		
		if(obj.getLocation() != null) {
			if(obj.getLocation().getId() == null) {
				locationService.save(obj.getLocation());
			}
			values.put("location_id", obj.getLocation().getId());
		} else {
            values.put("location_id", (Long) null);
        }
		
		return values;
	}

}
