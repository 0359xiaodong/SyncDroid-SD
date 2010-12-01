package de.syncdroid.transfer.impl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;

public class DropboxFileTransferClient extends AbstractFileTransferClient  {    
    final static private String CONSUMER_KEY = "cHV5cmp5aXA5dHkzYWFo";
    final static private String CONSUMER_SECRET = "bWtwZHczMHFiZnVpaHZ6";


    final static public String ACCOUNT_PREFS_NAME = "prefs";
    final static public String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static public String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    private static Context context;

    private static DropboxAPI api = new DropboxAPI();
    private static Config mConfig;


    public static boolean isAuthenticated() {
        Log.i(TAG, "dropbox isAuthenticated called");
        return getKeys() != null;
    }

    public static boolean authenticate(String username, String password) {
        Log.i(TAG, "dropbox authenticate called");
        mConfig = api.authenticate(getConfig(),
                username, password);

        setConfig(mConfig);

        Integer status = mConfig.authStatus;

        if (status != DropboxAPI.STATUS_SUCCESS) {
            Log.e(TAG, "could not authenticate to dropbox!");
            return false;
        }
        
    	storeKeys(mConfig.accessTokenKey, mConfig.accessTokenSecret);         

        return true;
    }
    
	public boolean connect() {
		String[] keys = getKeys();
		
		if(keys == null) {
    		Log.e(TAG, "connect called without stored keys!");
            return false;
		}
		
    	if (!api.isAuthenticated()) {
    		// authenticate with stored token
            mConfig = api.authenticateToken(keys[0], keys[1], getConfig());
    		
            Integer status = mConfig.authStatus;

            if (status != DropboxAPI.STATUS_SUCCESS) {
                Log.e(TAG, "could not authenticate to dropbox with stored keys!");
                Toast.makeText(context, "could not authenticate to dropbox " +
                		"with stored keys! " +
                		"clearing dropbox authentication keys ...", 2000).show();
                clearKeys();
                return false;
            }
    	}

    	Log.i(TAG, "dropbox connect successfull");
		return true;
	}
	
	public boolean transfer(File file, String targetPath) {
		if(!api.isAuthenticated()) {
        	Log.e(TAG, "transfer() called without dropbox authorisation");
        	return false;
		}
		
        String dirPath = null;

        if(targetPath.contains("/")) {
            dirPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
        }

        try {
        	String path = dirPath != null ? dirPath : "/";
        	
        	if(path.startsWith("/") == false) {
        		path = "/" + path;
        	}
        	
            int result = api.putFile("dropbox", path, file);
            Log.i(TAG, "dropbox transfer to '" + targetPath + "' " + (result == 1 ? "was successful" : "FAILED with code = " + result));
            return result == 1;
        } catch (Exception e) {
            Log.e(TAG, "dropbox transfer failed for '" + targetPath + "'", e);
            return false;
        }
	}

	public boolean disconnect() {
        Log.i(TAG, "dropbox disconnect called");
		api.deauthenticate();
		return true;
	}

    @Override
    protected boolean mkdir(String name) throws IOException {
        throw new RuntimeException("not implemented!");
    }

    @Override
    protected boolean cwd(String name) throws IOException {
        throw new RuntimeException("not implemented!");
    }

    protected static Config getConfig() {
    	if (mConfig == null) {
	    	mConfig = api.getConfig(null, false);

            /**
             * well, knowing there is no secure way to obfuscate something 
             * 	(especially in open source software)
             * i didn't try very hard ...
             */
            try {
                mConfig.consumerKey = new String(Base64.decodeBase64(
                		CONSUMER_KEY.getBytes()));
                mConfig.consumerSecret = new String(Base64.decodeBase64(
                		CONSUMER_SECRET.getBytes()));
            } catch(Exception e) {
                Log.e(TAG, "error deobfuscating consumer key/secret", e);
            }

	    	mConfig.server="api.dropbox.com";
	    	mConfig.contentServer="api-content.dropbox.com";
	    	mConfig.port=80;
    	}
    	return mConfig;
    }
    
    public static void setConfig(Config conf) {
    	mConfig = conf;
    }
	
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     * 
     * @return Array of [access_key, access_secret], or null if none stored
     */
    public static String[] getKeys() {
        SharedPreferences prefs = 
        	context.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
        	String[] ret = new String[2];
        	ret[0] = key;
        	ret[1] = secret;
        	return ret;
        } else {
        	return null;
        }
    }
    
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    public static void storeKeys(String key, String secret) {
        // Save the access key for later
        SharedPreferences prefs = context.getSharedPreferences(
        		ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(ACCESS_KEY_NAME, key);
        edit.putString(ACCESS_SECRET_NAME, secret);
        edit.commit();
    }
    
    public static void clearKeys() {
        SharedPreferences prefs = context.getSharedPreferences(
        		ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }

    public static void setContext(Context context) {
		DropboxFileTransferClient.context = context;
	}}
