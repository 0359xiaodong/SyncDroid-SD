package de.syncdroid.transfer.impl;

import android.util.Log;
import de.syncdroid.Utils;
import de.syncdroid.transfer.FileTransferClient;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: arturh
 * Date: 28.11.10
 * Time: 19:08
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractFileTransferClient implements FileTransferClient {
    protected static final String TAG = "FileTransferClient";

    abstract protected boolean mkdir(String name) throws IOException;
    abstract protected boolean cwd(String name) throws IOException;

    protected String currentWorkingDirectory = null;

    protected boolean createFolderStructure(String dirPath) throws IOException {
        if(mkdir(dirPath)) {
            Log.i(TAG, "created directory: " + Utils.combinePath(currentWorkingDirectory, dirPath));
        } else {// if this fails we assume the directory does not exist. so we create it:
            String remains = dirPath;

            // remains = /path/to/my/file

            while(remains != null) {
                String parent;
                String rest;

                if(remains.startsWith("/")) {
                    Log.d(TAG, "detected / at beginning, cwd to /...");
                    cwd("/");
                    currentWorkingDirectory = "/";

                    remains = remains.substring(1);

                    if("".equals(remains)) {
                        break;
                    }
                }

                if(remains.contains("/")){
                    parent = remains.substring(0, remains.indexOf('/'));
                    rest = remains.substring(remains.indexOf('/') + 1);
                } else {
                    parent = remains;
                    rest = null;
                }

                Log.d(TAG, "createFolderStructure() remains: '" + remains + "'");
                Log.d(TAG, "createFolderStructure() parent: '" + parent + "'");
                Log.d(TAG, "createFolderStructure() rest: '" + rest + "'");

                if(parent != null && "".equals(parent)) {
                    return true;
                }

                if(cwd(parent)) {
                    remains = rest;
                    currentWorkingDirectory = Utils.combinePath(currentWorkingDirectory, parent);
                    Log.i(TAG, "cwd to : " + currentWorkingDirectory);

                } else if(!mkdir(parent)) {
                    Log.e(TAG, "failed to create directory, giving up '" +
                            Utils.combinePath(currentWorkingDirectory, parent) + "'");

                    return false;
                } else {
                    Log.i(TAG, "created directory: " + Utils.combinePath(currentWorkingDirectory,
                            parent));

                    remains = rest;
                    if(!cwd(parent)) {
                        Log.e(TAG, "could not change into newly created directory :-(");
                        return false;
                    }
                    currentWorkingDirectory = Utils.combinePath(currentWorkingDirectory, parent);
                }
            }
        }
        return true;
    }
}
