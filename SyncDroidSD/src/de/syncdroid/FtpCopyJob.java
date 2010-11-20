package de.syncdroid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class FtpCopyJob implements Job {
	private static final String TAG = "FtpCopyJob";
	
	private String host;
	private String username;
	private String password;
	
	private String localPath;
	private String remotePath;
	public static final String PREF_FTP_PATH = "pref_ftp_path";
	public static final String PREF_FTP_PASSWORD = "pref_ftp_password";
	public static final String PREF_FTP_USERNAMAE = "pref_ftp_username";
	public static final String PREF_FTP_HOST = "pref_ftp_host";
	public static final String PREF_LOCAL_DIRECTORY = "pref_local_directory";
	
	public FtpCopyJob(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(
				ProfileActivity.TAG, Activity.MODE_PRIVATE);

        localPath = prefs.getString(FtpCopyJob.PREF_LOCAL_DIRECTORY, "");
        host = prefs.getString(FtpCopyJob.PREF_FTP_HOST, "");
        username = prefs.getString(FtpCopyJob.PREF_FTP_USERNAMAE, "");
        password = prefs.getString(FtpCopyJob.PREF_FTP_PASSWORD, "");
        remotePath = prefs.getString(FtpCopyJob.PREF_FTP_PATH, "");
        
        if(remotePath.startsWith("/")) {
        	remotePath = remotePath.substring(1);
        }
	}
	
	@Override
	public void execute() {
		try {
			// connect to ftp server
			FTPClient ftpClient = new FTPClient();
			ftpClient.connect(InetAddress.getByName(host));
			ftpClient.login(username, password);
			ftpClient.changeWorkingDirectory(remotePath);
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			
			File localFile = new File(localPath);
			
			BufferedInputStream inputStream=null;
			inputStream = new BufferedInputStream(
					new FileInputStream(localFile));
			ftpClient.enterLocalPassiveMode();
			
			String filename = localFile.getName();
			Log.i(TAG, "storing file " + filename);
			ftpClient.storeFile(filename, inputStream);
			
			inputStream.close();
			
			// disconnect from ftp server
			ftpClient.logout();
			ftpClient.disconnect();
		} catch(Exception e) {

			Log.e(TAG, "whoa, exception: " + e);
			
			 /*
			 AlertDialog.Builder adb = new AlertDialog.Builder(this);
			 
			 Throwable cause = e;
			 while(cause.getCause() != null) {
				 cause = cause.getCause();
			 }
			 
			 adb.setMessage(cause.getClass().getSimpleName() + ": " + cause.getMessage());
			 adb.setNegativeButton("whatever", null);
			 adb.show();*/
		}

	}

	public String getLocalDirectory() {
		return localPath;
	}

	public void setLocalDirectory(String localDirectory) {
		this.localPath = localDirectory;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUser() {
		return username;
	}

	public void setUser(String user) {
		this.username = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPath() {
		return remotePath;
	}

	public void setPath(String path) {
		this.remotePath = path;
	}

}
