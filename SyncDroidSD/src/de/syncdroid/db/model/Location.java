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
