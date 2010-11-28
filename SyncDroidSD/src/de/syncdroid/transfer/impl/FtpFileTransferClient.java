package de.syncdroid.transfer.impl;

import android.util.Log;
import de.syncdroid.Utils;
import de.syncdroid.transfer.FileTransferClient;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

public class FtpFileTransferClient extends AbstractFileTransferClient {
    private String hostname;
    private String username;
    private String password;

    private FTPClient ftpClient;

    public FtpFileTransferClient(String hostname, String username, String password) {
        this.hostname = hostname;
        this.username = username;
        this.password = password;
    }

    public boolean connect() {
        try {
            Log.i(TAG, "ftp connect to '" + username + "@" + hostname + "'");
			// connect to ftp server
            ftpClient = new FTPClient();
            ftpClient.connect(InetAddress.getByName(hostname));
            if(!ftpClient.login(username, password)) {
                Log.e(TAG, "ftp login failed for'" + username + "@" + hostname + "'");
                return false;
            }

            currentWorkingDirectory = null;

			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        } catch (IOException e) {
            Log.e(TAG, "ftp connect failed", e);
            return false;
        }
        return true;
    }

    public boolean transfer(File file, String targetPath) {
        String dirPath = null;
        String filename = targetPath;

        if(targetPath.contains("/")) {
            dirPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
            filename = targetPath.substring(targetPath.lastIndexOf('/') + 1);
        }

        try {
            if(currentWorkingDirectory != dirPath) {
                if(dirPath == null) {
                   dirPath = "";
                }

                if(ftpClient.changeWorkingDirectory(dirPath)) {
                    currentWorkingDirectory = dirPath;
                } else {
                    if(!createFolderStructure(dirPath)) {
                        return false;
                    }
                }
            }

            ftpClient.enterLocalPassiveMode();
            boolean result = ftpClient.storeFile(filename, new FileInputStream(file));
            Log.i(TAG, "ftp transfer to '" + targetPath + "' " + (result ? "was successful" : "FAILED"));
            return result;
        } catch (IOException e) {
            Log.e(TAG, "ftp transfer failed for '" + targetPath + "'", e);
            return false;
        }
    }

    public boolean disconnect() {
        Log.i(TAG, "ftp disconnect from '" + username + "@" + hostname + "'");
        try {
            // disconnect from ftp server
            if(!ftpClient.logout()) {
                Log.e(TAG, "ftp logout failed");
            }
            ftpClient.disconnect();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "ftp disconnect failed", e);
            return false;
        }
    }

    @Override
    protected boolean mkdir(String name) throws IOException {
        return ftpClient.makeDirectory(name);
    }

    @Override
    protected boolean cwd(String name) throws IOException {
        return ftpClient.changeWorkingDirectory(name);
    }
}
