package de.syncdroid.db.model.enums;

public enum ProfileStatusLevel {
    INFO("INFO"), WARN("WARN"), ERROR("ERROR"), SUCCESS("SUCCESS");

	ProfileStatusLevel(String code) {
		this.code = code;
	}

	private String code;

	@Override
	public String toString() {
		return this.code;
	}

	public static ProfileStatusLevel getByCode(String code) {
		if("INFO".equals(code))
			return INFO;
		if("WARN".equals(code))
			return WARN;
		if("ERROR".equals(code))
			return ERROR;
		if("SUCCESS".equals(code))
			return SUCCESS;

		return null;
	}
}

