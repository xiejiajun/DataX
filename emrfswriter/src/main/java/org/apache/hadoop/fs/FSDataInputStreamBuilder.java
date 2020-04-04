package org.apache.hadoop.fs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Evolving;
import org.apache.hadoop.conf.Configuration;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Public
@Evolving
public abstract class FSDataInputStreamBuilder<S extends FSDataInputStream, B extends FSDataInputStreamBuilder<S, B>> {
    private final FileSystem fs;

    private final Path path;

    private int bufferSize;

    private final Configuration options = new Configuration(false);

    private final Set<String> mandatoryKeys = new HashSet<>();

    protected abstract B getThisBuilder();

    protected FSDataInputStreamBuilder(@Nonnull FileSystem fileSystem, @Nonnull Path p) {
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(p);
        this.fs = fileSystem;
        this.path = p;
        this.bufferSize = this.fs.getConf().getInt("io.file.buffer.size", 4096);
    }

    protected FileSystem getFS() {
        return this.fs;
    }

    protected Path getPath() {
        return this.path;
    }

    protected int getBufferSize() {
        return this.bufferSize;
    }

    public B bufferSize(int bufSize) {
        this.bufferSize = bufSize;
        return getThisBuilder();
    }

    public B opt(@Nonnull String key, @Nonnull String value) {
        this.mandatoryKeys.remove(key);
        this.options.set(key, value);
        return getThisBuilder();
    }

    public B opt(@Nonnull String key, boolean value) {
        this.mandatoryKeys.remove(key);
        this.options.setBoolean(key, value);
        return getThisBuilder();
    }

    public B opt(@Nonnull String key, int value) {
        this.mandatoryKeys.remove(key);
        this.options.setInt(key, value);
        return getThisBuilder();
    }

    public B opt(@Nonnull String key, float value) {
        this.mandatoryKeys.remove(key);
        this.options.setFloat(key, value);
        return getThisBuilder();
    }

    public B opt(@Nonnull String key, double value) {
        this.mandatoryKeys.remove(key);
        this.options.setDouble(key, value);
        return getThisBuilder();
    }

    public B opt(@Nonnull String key, @Nonnull String... values) {
        this.mandatoryKeys.remove(key);
        this.options.setStrings(key, values);
        return getThisBuilder();
    }

    public B must(@Nonnull String key, @Nonnull String value) {
        this.mandatoryKeys.add(key);
        this.options.set(key, value);
        return getThisBuilder();
    }

    public B must(@Nonnull String key, boolean value) {
        this.mandatoryKeys.add(key);
        this.options.setBoolean(key, value);
        return getThisBuilder();
    }

    public B must(@Nonnull String key, int value) {
        this.mandatoryKeys.add(key);
        this.options.setInt(key, value);
        return getThisBuilder();
    }

    public B must(@Nonnull String key, float value) {
        this.mandatoryKeys.add(key);
        this.options.setFloat(key, value);
        return getThisBuilder();
    }

    public B must(@Nonnull String key, double value) {
        this.mandatoryKeys.add(key);
        this.options.setDouble(key, value);
        return getThisBuilder();
    }

    public B must(@Nonnull String key, @Nonnull String... values) {
        this.mandatoryKeys.add(key);
        this.options.setStrings(key, values);
        return getThisBuilder();
    }

    protected Configuration getOptions() {
        return this.options;
    }

    @VisibleForTesting
    protected Set<String> getMandatoryKeys() {
        return Collections.unmodifiableSet(this.mandatoryKeys);
    }

    public abstract S build() throws IllegalArgumentException, IOException;
}
