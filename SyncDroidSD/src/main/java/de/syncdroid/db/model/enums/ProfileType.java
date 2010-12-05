package de.syncdroid.db.model.enums;

public enum ProfileType {
	FTP("FTP"),
    SCP("SFTP / SCP"),
    //FTPs("FTPs"),
    SMB("SMB"),
    DROPBOX("Dropbox");
	
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
		if("SFTP / SCP".equals(code))
			return SCP;
		/*if("FTPs".equals(code))
			return FTPs;*/
		if("SMB".equals(code))
			return SMB;
		if("Dropbox".equals(code))
			return DROPBOX;

		return null;
	}
}
