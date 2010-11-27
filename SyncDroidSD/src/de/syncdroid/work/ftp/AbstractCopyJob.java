package de.syncdroid.work.ftp;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: arturh
 * Date: 27.11.10
 * Time: 23:17
 * To change this template use File | Settings | File Templates.
 */
public class AbstractCopyJob {
	private static final String TAG = "FtpCopyJob";

	protected Integer transferedFiles;
	protected Integer filesToTransfer;

    protected class RemoteFile {
        public Boolean isDirectory;
        public String name;
        public String fullpath;
        public File source;
        public Long newest;
        public List<RemoteFile> children = new ArrayList<OneWayFtpCopyJob.RemoteFile>();
    }

    protected RemoteFile buildTree(File dir, String fullpath) {
        Log.i(TAG, "buildTree with fullpath: '" + fullpath + "'");
        RemoteFile here = new RemoteFile();
        here.isDirectory = true;
        here.name = dir.getName();
        here.fullpath = fullpath;
        here.source = dir;
        here.newest = 0L;

        for (String item : dir.list()) {
            File fileItem = new File(dir, item);
            if (fileItem.isDirectory()) {
                RemoteFile tmp = buildTree(fileItem, fullpath + "/" + item);
                here.children.add(tmp);
                Log.d(TAG, " - adding: " + tmp.name);
                here.newest = Math.max(here.newest, tmp.newest);
            } else {
                RemoteFile aFile = new RemoteFile();
                aFile.isDirectory = false;
                aFile.name = item;
                aFile.source = fileItem;
                aFile.fullpath = fullpath;
                here.children.add(aFile);
                filesToTransfer ++;
                here.newest = Math.max(here.newest, fileItem.lastModified());
            }
        }

        return here;
    }

}
