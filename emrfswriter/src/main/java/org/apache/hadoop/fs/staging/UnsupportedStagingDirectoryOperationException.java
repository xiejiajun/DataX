package org.apache.hadoop.fs.staging;

import java.io.IOException;

public class UnsupportedStagingDirectoryOperationException extends IOException {
    public UnsupportedStagingDirectoryOperationException(String message) {
        super(message);
    }
}
