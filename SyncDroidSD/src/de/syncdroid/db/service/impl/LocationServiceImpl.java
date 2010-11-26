package de.syncdroid.db.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.syncdroid.db.model.Location;
import de.syncdroid.db.model.LocationCell;
import de.syncdroid.db.service.LocationCellService;
import de.syncdroid.db.service.LocationService;

public class LocationServiceImpl extends AbstractServiceImpl<Location> implements LocationService {
	@Inject private LocationCellService locationCellService; 

	@Override
	protected Location read(Cursor cursor) {
		Location obj = new Location();
		obj.setId(cursor.getLong(cursor.getColumnIndex("id")));
		obj.setName(cursor.getString(cursor.getColumnIndex("name")));
		
		
		List<LocationCell> cells = locationCellService.findAllbyLocation(obj);
		obj.setLocationCells(cells);	
		
		return obj;
	}
	
	private void _updateLocationCells(Location obj) {
		if(obj.getLocationCells() != null) {
			for(LocationCell locationCell  : obj.getLocationCells()) {
				locationCellService.saveOrUpdate(locationCell);
			}
		}
	}

	@Override
	public void save(Location obj) {
		super.save(obj);
		_updateLocationCells(obj);
	}
	
	@Override
	public void update(Location obj) {
		super.update(obj);
		_updateLocationCells(obj);
	}
	
	@Override
	protected ContentValues write(Location obj) {
		ContentValues values = new ContentValues();
		values.put("id", obj.getId());
		values.put("name", obj.getName());
		
		return values;
	}

	@Override
	protected String getTableName() {
		return "locations";
	}
	
	@Override
	public List<Location> locate(Integer cid, Integer lac) {
		List<LocationCell> locationCells =
			locationCellService.findByCidAndLac(cid, lac);
		
		List<Location> locations = new ArrayList<Location>();
		
		if(locationCells != null) {
			for(LocationCell cell : locationCells) {
				Location location = this.findById(cell.getLocationId());
				if(location != null) {
					locations.add(location);
				}
			}
		}
		
		return locations;
	}

}
