/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.syncdroid.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;

import de.syncdroid.db.model.Profile;
import de.syncdroid.db.service.LocationService;
import de.syncdroid.db.service.ProfileService;
import de.syncdroid.work.OneWayCopyJob;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncAdapter";

    private final AccountManager mAccountManager;
    private final Context mContext;
    private ProfileService profileService;
    private LocationService locationService;

    private Date mLastUpdated;

    public SyncAdapter(Context context, boolean autoInitialize, 
    		ProfileService profileService, LocationService locationService) {
        super(context, autoInitialize);
        mContext = context;
        this.profileService = profileService;
        this.locationService = locationService;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
        
		if(profileService != null) {
			Looper.prepare();
			for(Profile profile : profileService.list()) {
                new OneWayCopyJob(
                        mContext, profile,
                        profileService, locationService).run();
			}
		} else {
			Log.e(TAG, "profileService is NULL");
		}
    }
}
