package de.syncdroid;

import roboguice.config.AbstractAndroidModule;
import android.content.Context;
import de.syncdroid.db.service.LocationCellService;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.db.service.impl.LocationCellServiceImpl;
import de.syncdroid.db.service.impl.LocationServiceImpl;
import de.syncdroid.db.service.impl.ProfileServiceImpl;

public class SyncModule extends AbstractAndroidModule {
	private Context context;
	
	public SyncModule(Context context) {
		this.context = context;
	}	
	
    @Override
    protected void configure() {
        bind(ProfileService.class).to(ProfileServiceImpl.class);     
        bind(LocationService.class).to(LocationServiceImpl.class);  
        bind(LocationCellService.class).to(LocationCellServiceImpl.class);
        
        DatabaseHelper helper = new DatabaseHelper(context);
        bind(DatabaseHelper.class).toInstance(helper);
    }

}
