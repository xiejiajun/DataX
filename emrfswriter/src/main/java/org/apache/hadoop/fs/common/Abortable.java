package org.apache.hadoop.fs.common;

import java.io.IOException;

public interface Abortable {
    void abort() throws IOException;
}
