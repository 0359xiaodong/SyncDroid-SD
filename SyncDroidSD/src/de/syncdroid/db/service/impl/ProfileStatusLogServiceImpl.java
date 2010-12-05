package de.syncdroid.db.service.impl;

import java.text.ParseException;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.inject.Inject;

import de.syncdroid.db.model.Profile;
import de.syncdroid.db.model.ProfileStatusLog;
import de.syncdroid.db.model.enums.ProfileStatusLevel;
import de.syncdroid.db.model.enums.ProfileStatusType;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.db.service.ProfileStatusLogService;

public class ProfileStatusLogServiceImpl extends AbstractServiceImpl<ProfileStatusLog> implements ProfileStatusLogService {
	private static final String TAG = "SyncDroid.ProfileStatusLogServiceImpl";

    @Inject
    private ProfileService profileService;


    /**
    private String shortMessage;
	private String detailMessage;
    private Date timestamp;
    private ProfileStatusLevel statusLevel;
    private ProfileStatusType profileStatusType;
    private String localFilePath;
    private Profile profile;
     */
	@Override
	protected ProfileStatusLog read(Cursor cursor) {
		ProfileStatusLog obj = new ProfileStatusLog();
		obj.setId(cursor.getLong(cursor.getColumnIndex("id")));
		obj.setShortMessage(cursor.getString(cursor.getColumnIndex("short_message")));
		obj.setDetailMessage(cursor.getString(cursor.getColumnIndex("detail_message")));

		String dateString =
			cursor.getString(cursor.getColumnIndex("timestamp"));
		try {
			if(dateString != null && "".equals(dateString) == false) {
				obj.setTimestamp(dateFormat.parse(dateString));
			}
		} catch (ParseException e) {
			Log.e(TAG, "parseException for: " + dateString);
		}

        obj.setProfileStatusType(ProfileStatusType.getByCode(
                cursor.getString(cursor.getColumnIndex("profile_status_type"))));

        obj.setStatusLevel(ProfileStatusLevel.getByCode(
                cursor.getString(cursor.getColumnIndex("profile_status_level"))));

		obj.setLocalFilePath(cursor.getString(cursor.getColumnIndex("local_file_path")));

		Long profileId = cursor.getLong(cursor.getColumnIndex("profile_id"));

		if(profileId != null && profileId != 0) {
			obj.setProfile(profileService.findById(profileId));
		}
		return obj;
	}

	@Override
	protected ContentValues write(ProfileStatusLog obj) {
		ContentValues values = new ContentValues();
		values.put("id", obj.getId());
		values.put("short_message", obj.getShortMessage());
		values.put("detail_message", obj.getDetailMessage());

        values.put("timestamp", obj.getTimestamp() == null ? null :
			dateFormat.format(obj.getTimestamp()));

		values.put("profile_status_type", obj.getProfileStatusType() != null ?
                obj.getProfileStatusType().toString() : null);

		values.put("profile_status_level", obj.getStatusLevel() != null ?
                obj.getStatusLevel().toString() : null);

		values.put("local_file_path", obj.getLocalFilePath());

        if(obj.getProfile() != null) {
            if(obj.getProfile().getId() == null) {
                profileService.save(obj.getProfile());
            }
            values.put("profile_id", obj.getProfile().getId());
        } else {
            values.put("profile_id", (Long) null);
        }

		return values;
	}

	public List<ProfileStatusLog> list() {
		SQLiteDatabase db = databaseHelper.getReadableDatabase();
		Cursor cursor = db.query(getTableName(), null,
				null, null,
				null, null, "timestamp DESC", null);

		List<ProfileStatusLog> lst = _list(cursor);
		db.close();

		return lst;
	}

	@Override
	protected String getTableName() {
		return "profile_status_logs";
	}

    @Override
    public ProfileStatusLog findLatestByProfile(Profile profile) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        // (boolean distinct, String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String orderBy, String limit)
		Cursor cursor = db.query(true, getTableName(), null,
				"profile_id = ?", new String[] {profile.getId().toString()},
				null, null, "timestamp desc", "1");

		if (cursor == null || cursor.moveToFirst() == false) {
			return null;
		}

		ProfileStatusLog obj = read(cursor);

		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

		db.close();

		return obj;
    }

    @Override
    public void deleteAll() {
		SQLiteDatabase db = databaseHelper.getWritableDatabase();
		db.delete(getTableName(), null, null);
		db.close();
    }


}
