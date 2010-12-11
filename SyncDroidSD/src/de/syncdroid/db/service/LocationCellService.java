package de.syncdroid.db.service;

import java.util.List;

import de.syncdroid.db.model.Location;
import de.syncdroid.db.model.LocationCell;

public interface LocationCellService extends Service<LocationCell> {
	List<LocationCell> findAllbyLocation(Location location);
	List<LocationCell> findByCidAndLac(Integer cid, Integer lac);
}
