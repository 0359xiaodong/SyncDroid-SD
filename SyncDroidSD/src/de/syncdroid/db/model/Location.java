package de.syncdroid.db.model;

import java.util.ArrayList;
import java.util.List;

public class Location implements Model {
	public Location() {
		locationCells = new ArrayList<LocationCell>();
	}
	private Long id;
	private String name;
	
	private List<LocationCell> locationCells;

	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public Long getId() {
		return id;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		
		// object must be Test at this point
		Location test = (Location) obj;
		return 
			(id != null && id.equals(test.id)) 
			;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (null == id ? 0 : id.hashCode());
		return hash;
	}	
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<LocationCell> getLocationCells() {
		return locationCells;
	}

	public void setLocationCells(List<LocationCell> locationCells) {
		this.locationCells = locationCells;
	}

}
