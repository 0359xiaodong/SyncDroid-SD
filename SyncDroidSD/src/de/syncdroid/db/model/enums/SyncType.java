package de.syncdroid.db.model.enums;

public enum SyncType {
	ONE_WAY("One Way");

	SyncType(String code) {
		this.code = code;
	}

	private String code;
	
	@Override
	public String toString() {
		return this.code;
	}
		
	public static SyncType getByCode(String code) {
		if("One Way".equals(code))
			return ONE_WAY;

		return null;
	}
}
