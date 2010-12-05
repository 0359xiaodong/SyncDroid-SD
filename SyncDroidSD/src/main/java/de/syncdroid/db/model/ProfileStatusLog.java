package de.syncdroid.db.model;

import de.syncdroid.db.model.enums.ProfileStatusLevel;
import de.syncdroid.db.model.enums.ProfileStatusType;

import java.util.Date;

@SuppressWarnings("serial")
public class ProfileStatusLog implements Model {
	private Long id;
	private String shortMessage;
	private String detailMessage;
    private Date timestamp;
    private ProfileStatusLevel statusLevel;
    private ProfileStatusType profileStatusType;
    private String localFilePath;
    private Profile profile;

	@Override
	public String toString() {
		return shortMessage;
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
		ProfileStatusLog test = (ProfileStatusLog) obj;
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

    public String getShortMessage() {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public ProfileStatusLevel getStatusLevel() {
        return statusLevel;
    }

    public void setStatusLevel(ProfileStatusLevel statusLevel) {
        this.statusLevel = statusLevel;
    }

    public ProfileStatusType getProfileStatusType() {
        return profileStatusType;
    }

    public void setProfileStatusType(ProfileStatusType profileStatusType) {
        this.profileStatusType = profileStatusType;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }
}
