package de.syncdroid.db.service;

import java.util.List;

import de.syncdroid.db.model.Location;

public interface LocationService extends Service<Location> {
	List<Location> locate(Integer cid, Integer loc);	
}
