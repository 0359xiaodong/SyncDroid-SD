package de.syncdroid.transfer;

import java.io.File;
import java.io.InputStream;

public interface FileTransferClient {
    boolean connect();
    boolean transfer(InputStream inputStream, String targetPath);
    boolean disconnect();
}
