package org.apache.hadoop.fs.staging;

import org.apache.hadoop.fs.Path;

import java.io.FileNotFoundException;

public class StagingDirectoryNotFoundException extends FileNotFoundException {
    public StagingDirectoryNotFoundException(Path destinationPath, String stageName) {
        super("Staging directory not found under path " + destinationPath + " with stage name \"" + stageName + "\"");
    }
}
