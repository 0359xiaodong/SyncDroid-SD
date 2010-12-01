package de.syncdroid;

import java.util.List;

import roboguice.application.GuiceApplication;

import com.google.inject.Module;

import de.syncdroid.transfer.impl.DropboxFileTransferClient;


public class SyncApplication extends GuiceApplication {
	@Override
	public void onCreate() {
		super.onCreate();		
		DropboxFileTransferClient.setContext(this);
	}
	
	protected void addApplicationModules(List<Module> modules) {
		modules.add(new SyncModule(this));
	}
}
