package de.syncdroid.db.service;

import de.syncdroid.db.model.Profile;
import de.syncdroid.db.model.ProfileStatusLog;

public interface ProfileStatusLogService extends Service<ProfileStatusLog> {
     ProfileStatusLog findLatestByProfile(Profile profile);
     void deleteAll();
}
