package de.syncdroid;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import de.syncdroid.R;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "syncdroid.db";
    private static final int DATABASE_VERSION = 6;
    private Context mContext;
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Log.d(TAG, "Constructor()");
		mContext = context;
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(TAG, "onCreate()");
		onUpgrade(db, 0, DATABASE_VERSION);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, String.format("onUpgrade(%s, %s)", oldVersion, newVersion));
		Resources res = mContext.getResources();
		
		if (oldVersion < 1) {
			Log.i(TAG, "update to version 1");
			db.execSQL(res.getString(R.string.create_table_profiles));
		}
		if (oldVersion < 2) {
			Log.i(TAG, "update to version 2");
			db.execSQL(res.getString(R.string.create_table_locations));
			db.execSQL(res.getString(R.string.create_table_locations_cells));
		}
		if (oldVersion < 3) {
			Log.i(TAG, "update to version 3");
			db.execSQL(res.getString(R.string.update001));
		}
		if (oldVersion < 4) {
			Log.i(TAG, "update to version 4");
			db.execSQL(res.getString(R.string.update002));
		}
		if (oldVersion < 5) {
			Log.i(TAG, "update to version 5");
			db.execSQL(res.getString(R.string.update003));
		}
		if (oldVersion < 6) {
			Log.i(TAG, "update to version 6");
			db.execSQL(res.getString(R.string.update004));
		}
		Log.i(TAG, "onUpgrade(), complete");
	}

}
