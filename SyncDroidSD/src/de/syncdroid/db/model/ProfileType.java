package de.syncdroid.db.model;

public enum ProfileType {
	FTP("FTP"), SFTP("SFTP"), FTPs("FTPs");
	
	ProfileType(String code) {
		this.code = code;
	}

	private String code;
	
	@Override
	public String toString() {
		return this.code;
	}
		
	public static ProfileType getByCode(String code) {
		if("FTP".equals(code)) 
			return FTP;
		if("SFTP".equals(code)) 
			return SFTP;
		if("FTPs".equals(code)) 
			return FTPs;

		return null;
	}
}
