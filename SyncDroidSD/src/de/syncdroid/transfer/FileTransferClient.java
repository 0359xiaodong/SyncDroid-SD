package de.syncdroid.transfer;

import java.io.File;

public interface FileTransferClient {
    boolean connect();
    boolean transfer(File file, String targetPath);
    boolean disconnect();
}
