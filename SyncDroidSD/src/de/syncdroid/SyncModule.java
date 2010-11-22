package de.syncdroid;

import roboguice.config.AbstractAndroidModule;
import android.content.Context;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.db.service.impl.ProfileServiceImpl;

public class SyncModule extends AbstractAndroidModule {
	private Context context;
	
	public SyncModule(Context context) {
		this.context = context;
	}	
	
    @Override
    protected void configure() {
        bind(ProfileService.class).to(ProfileServiceImpl.class);        
        DatabaseHelper helper = new DatabaseHelper(context);
        bind(DatabaseHelper.class).toInstance(helper);
    }

}
