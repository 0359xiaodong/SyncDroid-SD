package de.syncdroid.service;

import roboguice.application.GuiceApplication;
import roboguice.inject.ContextScope;
import roboguice.inject.InjectorProvider;
import android.app.Service;
import android.content.Intent;

import com.google.inject.Injector;

public abstract class GuiceService extends Service implements InjectorProvider {
	
    protected ContextScope scope;
    
    @Override
    public void onStart(Intent intent, int startId) {
        final Injector injector = getInjector();
        scope = injector.getInstance(ContextScope.class);
        scope.enter(this);
        injector.injectMembers(this);
        super.onStart(intent, startId);
    }

    @Override
    public Injector getInjector() {
        return ((GuiceApplication) getApplication()).getInjector();
    }

    
}
