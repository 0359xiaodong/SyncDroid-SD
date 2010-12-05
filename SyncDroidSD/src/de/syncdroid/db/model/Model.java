package de.syncdroid.db.model;

import java.io.Serializable;

public interface Model extends Serializable {
	Long getId();
	void setId(Long id);
}
