package org.apache.hadoop.fs.staging;

import org.apache.hadoop.fs.Path;

import javax.annotation.Nonnull;
import java.io.IOException;

public interface StagingDirectoryService {
    boolean isStagingDirectoryPath(@Nonnull Path paramPath);

    boolean hasStagingDirectory(@Nonnull Path paramPath, @Nonnull String paramString) throws IOException;

    Path makeStagingDirectory(@Nonnull Path paramPath, @Nonnull String paramString) throws IOException;

    void publishStagingDirectory(@Nonnull Path paramPath, @Nonnull String paramString) throws IOException;

    void deleteStagingDirectory(@Nonnull Path paramPath, @Nonnull String paramString) throws IOException;
}
