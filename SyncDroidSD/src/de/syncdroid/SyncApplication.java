package de.syncdroid;

import java.util.List;

import com.google.inject.Inject;
import de.syncdroid.db.service.ProfileStatusLogService;
import de.syncdroid.db.service.impl.ProfileStatusLogServiceImpl;
import roboguice.application.GuiceApplication;

import com.google.inject.Module;


public class SyncApplication extends GuiceApplication {
    protected void addApplicationModules(List<Module> modules) {
		modules.add(new SyncModule(this));
	}
}
