package de.syncdroid.db.model.enums;

public enum ProfileStatusType {
    FILE_DELETED("DELETED"),
    FILE_CREATED("CREATED"),
    FILE_COPIED("COPIED"),
    FILE_UPDATED("UPDATED")
    ;

	ProfileStatusType(String code) {
		this.code = code;
	}

	private String code;
	
	@Override
	public String toString() {
		return this.code;
	}
		
	public static ProfileStatusType getByCode(String code) {
		if("DELETED".equals(code))
			return FILE_DELETED;
		if("CREATED".equals(code))
			return FILE_CREATED;
		if("UPDATED".equals(code))
			return FILE_UPDATED;
		if("COPIED".equals(code))
			return FILE_COPIED;

		return null;
	}
}
