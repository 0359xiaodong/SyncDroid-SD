package de.syncdroid.db.service.impl;

import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.syncdroid.db.model.Location;
import de.syncdroid.db.model.LocationCell;
import de.syncdroid.db.service.LocationCellService;

public class LocationCellServiceImpl extends AbstractServiceImpl<LocationCell> implements LocationCellService {

	@Override
	protected LocationCell read(Cursor cursor) {
		LocationCell obj = new LocationCell();
		obj.setId(cursor.getLong(cursor.getColumnIndex("id")));
		obj.setCid(cursor.getInt(cursor.getColumnIndex("cid")));
		obj.setLac(cursor.getInt(cursor.getColumnIndex("lac")));
		obj.setLocationId(cursor.getLong(cursor.getColumnIndex("name")));
		
		return obj;
	}

	@Override
	protected ContentValues write(LocationCell obj) {
		ContentValues values = new ContentValues();
		values.put("id", obj.getId());
		values.put("cid", obj.getCid());
		values.put("lac", obj.getLac());
		values.put("location_id", obj.getLocationId());
		return values;
	}

	@Override
	protected String getTableName() {
		return "location_cells";
	}

	@Override
	public List<LocationCell> findAllbyLocation(Location location) {
		SQLiteDatabase db = databaseHelper.getReadableDatabase();
		Cursor cursor = db.query(getTableName(), null, 
				"location_id = ?", new String[] {location.getId().toString()}, 
				null, null, null, null);
		
		List<LocationCell> lst = _list(cursor);
		db.close();
		
		return lst;
	}

}
