package de.syncdroid.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.google.inject.Injector;

import roboguice.application.GuiceApplication;
import roboguice.inject.ContextScope;
import roboguice.inject.InjectorProvider;

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


    /**
     * @see GuiceApplication#getInjector()
     */
    public Injector getInjector() {
        return ((GuiceApplication) getApplication()).getInjector();
    }

    
}
