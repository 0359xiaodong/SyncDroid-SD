package de.syncdroid.transfer.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * Created by IntelliJ IDEA.
 * User: arturh
 * Date: 28.11.10
 * Time: 20:27
 * To change this template use File | Settings | File Templates.
 */
public class ScpFileTransferClient extends AbstractFileTransferClient {
    private String hostname;
    private String username;
    private String password;
    private Integer port;

    private Activity activity;

    private JSch jsch;
    private Session session;
    private ChannelSftp channel;

    public ScpFileTransferClient(String hostname, String username, 
    		String password, Integer port, Activity activity) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.port = port;
        this.activity = activity;
    }

    private void configureSession(Session session) {
        session.setConfig("StrictHostKeyChecking", "no");
    }

    public boolean connect() {
        try {
            Log.i(TAG, "scp connect to '" + username + "@" + hostname + "'");
            //Log.i(TAG, "password: '" + password + "'");
			// connect to ftp server
            jsch = new JSch();
            session = jsch.getSession(username, hostname, port != null 
            		? port : 22);
            configureSession(session);

            try {
                UserInfo ui = new MyUserInfo(password);
                session.setUserInfo(ui);
                session.connect();

                if(session.isConnected() == false) {
                    Log.e(TAG, "scp connect failed !!");
                }

                HostKeyRepository hkr = jsch.getHostKeyRepository();

                HostKey hk = session.getHostKey();
                String host = hk.getHost();
                String fingerPrint = hk.getFingerPrint(jsch);
                String key = hk.getKey();

                Log.i(TAG, "adding identify: host '" + host + 
                		"' with fingerprint '" + fingerPrint + "' " +
                        "and key '" + key + "'");

                hkr.add(hk, ui);
            } catch(JSchException e) {
                Log.e(TAG, "scp connect failed", e);
                return false;
            }

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            currentWorkingDirectory = null;
        } catch (JSchException e) {
            Log.e(TAG, "scp connect failed", e);
            return false;
        }
        return true;
    }

    public boolean transfer(File file, String targetPath) {
        Log.i(TAG, "scp transfer of '" + targetPath + "'");

        String dirPath = null;
        String filename = targetPath;

        if(targetPath.contains("/")) {
            dirPath = targetPath.substring(0, targetPath.lastIndexOf('/') + 1);
            filename = targetPath.substring(targetPath.lastIndexOf('/') + 1);
        }

        Log.i(TAG, "dirPath: '" + dirPath + "'");
        Log.i(TAG, "filename: '" + filename + "'");

        FileInputStream fis = null;
        try {
            if(currentWorkingDirectory != dirPath &&((currentWorkingDirectory == null || dirPath == null)
                    || (currentWorkingDirectory.equals(dirPath) == false))) {
                if(dirPath == null) {
                   dirPath = "";
                }

                if(cwd(dirPath)) {
                    currentWorkingDirectory = dirPath;
                } else {
                    if(!createFolderStructure(dirPath)) {
                        return false;
                    }
                }
            }


            Log.i(TAG, "creating stream");
            // send a content of lfile
            fis = new FileInputStream(file);

            Log.i(TAG, "putting file to '" + targetPath + "' (cwd: '" + currentWorkingDirectory + "')");
            channel.put(fis, targetPath, ChannelSftp.OVERWRITE);

            Log.i(TAG, "closing stream");
            fis.close();
            fis=null;

            Log.i(TAG, "scp transfer successful for '" + targetPath + "'");

            return true;
        } catch(Exception e) {
            Log.e(TAG, "scp transfer failed for '" + targetPath + "'", e);
            return false;
        }  finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "closing stuff failed: ", e);
                }
            }
        }
    }

    public boolean disconnect() {
        Log.i(TAG, "scp disconnect from '" + username + "@" + hostname + "'");
        channel.disconnect();
        session.disconnect();
        return true;
    }

    @Override
    protected boolean mkdir(String name) throws IOException {
        try {
            channel.mkdir(name);
        } catch (SftpException e) {
            Log.e(TAG, "mkdir(" + name + ") FAILED", e);
            return false;
        }
        Log.d(TAG, "mkdir(" + name + ") ok");
        return true;
    }

    @Override
    protected boolean cwd(String name) throws IOException {
        try {
            channel.cd(name);
        } catch (SftpException e) {
            Log.e(TAG, "cwd(" + name + ") FAILED", e);
            return false;
        }
        Log.d(TAG, "cwd(" + name + ") ok");
        return true;
    }

    public class MyUserInfo implements UserInfo, UIKeyboardInteractive{
        private String password;

        public MyUserInfo(String password) {
            this.password = password;
        }

        public String getPassword() {
            Log.d(TAG, "getPassword()");
            return password;
        }

        public String getPassphrase() {
            Log.d(TAG, "getPassphrase()");
            return password;
        }

        public boolean promptPassphrase(String message){
            Log.d(TAG, "promptPassphrase(" + message + ")");
            return true;
        }

        private Boolean result;

        private void setResult(boolean result) {
            this.result = result;
        }

        public boolean promptYesNo(String s) {
            Log.d(TAG, "promtYesNo(" + s + ")");

            if(activity != null) {
                synchronized (result) {
                    result = null;

                    AlertDialog.Builder adb = new AlertDialog.Builder(activity);
                    adb.setMessage(s);
                    adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setResult(true);
                        }
                    });
                    adb.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setResult(false);
                        }
                    });
                    adb.show();

                    while(result == null) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                    }
                }

                return result;
            } else {
                return true;
            }
        }

        public boolean promptPassword(String message){
            Log.d(TAG, "promptPassword(" + message + ")");
            return true;
        }

        public void showMessage(String message){
            Log.d(TAG, "showMessage(" + message + ")");

            if(activity != null) {
                AlertDialog.Builder adb=new AlertDialog.Builder(activity);
                adb.setMessage(message);
                adb.show();
            }
        }

        public String[] promptKeyboardInteractive(String destination,
                String name,
                String instruction,
                String[] prompt,
                boolean[] echo){

            Log.d(TAG, "promptKeyboardInteractive()");

            List<String> responses = new ArrayList<String>();
            for(int i=0; i<prompt.length; i++){
                responses.add(password);
            }

            return responses.toArray(new String[]{});
        }
    }
}
