package de.syncdroid.transfer.impl;

import android.util.Log;
import de.syncdroid.Utils;
import de.syncdroid.transfer.FileTransferClient;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.InetAddress;

public class SmbFileTransferClient extends AbstractFileTransferClient {

    static final int BUFF_SIZE = 100000;
    static final byte[] buffer = new byte[BUFF_SIZE];


    private String hostname;
    private String domain;
    private String username;
    private String password;

    private String sharePath;
    private String pathPrefix;

    private NtlmPasswordAuthentication passwordAuthentication;

    public SmbFileTransferClient(String hostname, String domain, String username, String password) {
        this.hostname = hostname;
        this.domain = domain;
        this.username = username;
        this.password = password;
    }

    public boolean connect() {
        Log.i(TAG, "smb connect to '" + ((domain != null)?(domain + "\\"):(""))+ username + "@" + hostname + "'");
        passwordAuthentication = new NtlmPasswordAuthentication(domain, username, password);
        currentWorkingDirectory = null;
        return true;
    }

    public boolean transfer(InputStream inputStream, String targetPath) {
        if(targetPath.startsWith("/") == false) {
            Log.e(TAG, "invalid smb target path: '" + targetPath + "'");
            return false;
        }

        targetPath = targetPath.substring(1);

        if(targetPath.contains("/") == false) {
            Log.e(TAG, "invalid smb target path: '" + targetPath + "'");
            return false;
        }

        sharePath = targetPath.substring(0, targetPath.indexOf("/"));
        pathPrefix = "smb://" + hostname + "/" + sharePath + "/";

        targetPath = targetPath.substring(sharePath.length() + 1);
        String dirPath = null;
        String filename = targetPath;

        if(targetPath.contains("/")) {
            dirPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
            filename = targetPath.substring(targetPath.lastIndexOf('/') + 1);
        }

        Log.d(TAG, "sharePath= '" + sharePath + "' ");
        Log.d(TAG, "dirPath= '" + dirPath + "' ");
        Log.d(TAG, "filename= '" + filename + "' ");
        Log.d(TAG, "targetPath= '" + targetPath + "' ");
        Log.d(TAG, "pathPrefix= '" + pathPrefix + "' ");
        Log.d(TAG, "currentWorkingDirectory= '" + currentWorkingDirectory + "' ");

        try {
            if(currentWorkingDirectory != dirPath) {
                if(dirPath == null) {
                   dirPath = "";
                }

                Log.d(TAG, "changing directory ... ");
                if(cwd(dirPath)) {
                    Log.d(TAG, "worked!");
                    currentWorkingDirectory = dirPath;
                } else {
                    Log.d(TAG, "creating folder structure... ");
                    if(!createFolderStructure(dirPath)) {
                        Log.d(TAG, "FAILED");
                        return false;
                    }
                }
            }


            Log.d(TAG, "path= '" + pathPrefix + targetPath + "' ");
            SmbFile smbFile = new SmbFile(pathPrefix + targetPath, passwordAuthentication);

            SmbFileOutputStream outStream = new SmbFileOutputStream(smbFile);

            while (true) {
               synchronized (buffer) {
                  int amountRead = inputStream.read(buffer);
                  if (amountRead == -1) {
                     break;
                  }
                  outStream.write(buffer, 0, amountRead);
               }
            }

            inputStream.close();
            outStream.close();

            Log.i(TAG, "smb transfer to '" + targetPath + "' was successful");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "smb transfer failed for '" + targetPath + "'", e);
            return false;
        }
    }

    public boolean disconnect() {
        Log.i(TAG, "smb disconnect from '" + ((domain != null)?(domain + "\\"):(""))+ username + "@" + hostname + "'");
        return true;
    }

    @Override
    protected boolean mkdir(String name) throws IOException {
        String path = null;

        if(name.contains("/")) {
            path = pathPrefix + name;
        } else {
            path = pathPrefix + Utils.combinePath(currentWorkingDirectory, name);
        }

        Log.d(TAG, "mkdir('" + path + "')");
        SmbFile smbFile = new SmbFile(path, passwordAuthentication);

        try {
            smbFile.mkdir();
        } catch(Exception e) {
            Log.d(TAG, "mkdir('" + path + "') result: " + false);
            return false;
        }

        Log.d(TAG, "mkdir('" + path + "') result: " + true);
        return true;
    }

    @Override
    protected boolean cwd(String name) throws IOException {
        String path = null;

        if(name.contains("/")) {
            path = pathPrefix + name;
        } else {
            path = pathPrefix + Utils.combinePath(currentWorkingDirectory, name);
        }

        Log.d(TAG, "cwd('" + path + "')");
        SmbFile smbFile = new SmbFile(path, passwordAuthentication);

        boolean result = smbFile.exists() && smbFile.isDirectory();
        Log.d(TAG, "cwd('" + path + "') result: " + result);
        return result;
    }
}
