//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.apache.hadoop.hive.ql.io.orc;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.AcidOutputFormat;
import org.apache.hadoop.hive.ql.io.AcidUtils;
import org.apache.hadoop.hive.ql.io.RecordUpdater;
import org.apache.hadoop.hive.ql.io.StatsProvidingRecordWriter;
import org.apache.hadoop.hive.ql.io.AcidOutputFormat.Options;
import org.apache.hadoop.hive.ql.io.orc.OrcFile.EncodingStrategy;
import org.apache.hadoop.hive.ql.io.orc.OrcFile.OrcTableProperties;
import org.apache.hadoop.hive.ql.io.orc.OrcFile.WriterOptions;
import org.apache.hadoop.hive.ql.io.orc.OrcRecordUpdater.KeyIndexBuilder;
import org.apache.hadoop.hive.ql.io.orc.OrcSerde.OrcSerdeRow;
import org.apache.hadoop.hive.serde2.SerDeStats;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Progressable;

public class OrcOutputFormat extends FileOutputFormat<NullWritable, OrcSerdeRow> implements AcidOutputFormat<NullWritable, OrcSerdeRow> {
    public OrcOutputFormat() {
    }

    private String getSettingFromPropsFallingBackToConf(String key, Properties props, JobConf conf) {
        if (props != null && props.containsKey(key)) {
            return props.getProperty(key);
        } else {
            return conf != null ? conf.get(key) : null;
        }
    }

    private WriterOptions getOptions(JobConf conf, Properties props) {
        WriterOptions options = OrcFile.writerOptions(conf);
        String propVal;
        if ((propVal = this.getSettingFromPropsFallingBackToConf(OrcTableProperties.STRIPE_SIZE.getPropName(), props, conf)) != null) {
            options.stripeSize(Long.parseLong(propVal));
        }

        if ((propVal = this.getSettingFromPropsFallingBackToConf(OrcTableProperties.COMPRESSION.getPropName(), props, conf)) != null) {
            options.compress(CompressionKind.valueOf(propVal));
        }

        if ((propVal = this.getSettingFromPropsFallingBackToConf(OrcTableProperties.COMPRESSION_BLOCK_SIZE.getPropName(), props, conf)) != null) {
            options.bufferSize(Integer.parseInt(propVal));
        }

        if ((propVal = this.getSettingFromPropsFallingBackToConf(OrcTableProperties.ROW_INDEX_STRIDE.getPropName(), props, conf)) != null) {
            options.rowIndexStride(Integer.parseInt(propVal));
        }

        if ((propVal = this.getSettingFromPropsFallingBackToConf(OrcTableProperties.ENABLE_INDEXES.getPropName(), props, conf)) != null && "false".equalsIgnoreCase(propVal)) {
            options.rowIndexStride(0);
        }

        if ((propVal = this.getSettingFromPropsFallingBackToConf(OrcTableProperties.BLOCK_PADDING.getPropName(), props, conf)) != null) {
            options.blockPadding(Boolean.parseBoolean(propVal));
        }

        if ((propVal = this.getSettingFromPropsFallingBackToConf(OrcTableProperties.ENCODING_STRATEGY.getPropName(), props, conf)) != null) {
            options.encodingStrategy(EncodingStrategy.valueOf(propVal));
        }

        return options;
    }

    public RecordWriter<NullWritable, OrcSerdeRow> getRecordWriter(FileSystem fileSystem, JobConf conf, String name, Progressable reporter) throws IOException {
        WriterOptions writerOptions = this.getOptions(conf, (Properties)null);
        writerOptions.fileSystem(fileSystem);
        return new OrcOutputFormat.OrcRecordWriter(new Path(name), writerOptions);
    }

    public StatsProvidingRecordWriter getHiveRecordWriter(JobConf conf, Path path, Class<? extends Writable> valueClass, boolean isCompressed, Properties tableProperties, Progressable reporter) throws IOException {
        return new OrcOutputFormat.OrcRecordWriter(path, this.getOptions(conf, tableProperties));
    }

    public RecordUpdater getRecordUpdater(Path path, Options options) throws IOException {
        return (RecordUpdater)(options.getDummyStream() != null ? new OrcOutputFormat.DummyOrcRecordUpdater(path, options) : new OrcRecordUpdater(path, options));
    }

    public org.apache.hadoop.hive.ql.exec.FileSinkOperator.RecordWriter getRawRecordWriter(Path path, Options options) throws IOException {
        Path filename = AcidUtils.createFilename(path, options);
        WriterOptions opts = OrcFile.writerOptions(options.getConfiguration());
        if (!options.isWritingBase()) {
            opts.bufferSize(16384).stripeSize(16777216L).blockPadding(false).compress(CompressionKind.NONE).rowIndexStride(0);
        }

        final KeyIndexBuilder watcher = new KeyIndexBuilder();
        opts.inspector(options.getInspector()).callback(watcher);
        final Writer writer = OrcFile.createWriter(filename, opts);
        return new org.apache.hadoop.hive.ql.exec.FileSinkOperator.RecordWriter() {
            public void write(Writable w) throws IOException {
                OrcStruct orc = (OrcStruct)w;
                watcher.addKey(((IntWritable)orc.getFieldValue(0)).get(), ((LongWritable)orc.getFieldValue(1)).get(), ((IntWritable)orc.getFieldValue(2)).get(), ((LongWritable)orc.getFieldValue(3)).get());
                writer.addRow(w);
            }

            public void close(boolean abort) throws IOException {
                writer.close();
            }
        };
    }

    private class DummyOrcRecordUpdater implements RecordUpdater {
        private final Path path;
        private final ObjectInspector inspector;
        private final PrintStream out;

        private DummyOrcRecordUpdater(Path path, Options options) {
            this.path = path;
            this.inspector = options.getInspector();
            this.out = options.getDummyStream();
        }

        public void insert(long currentTransaction, Object row) throws IOException {
            this.out.println("insert " + this.path + " currTxn: " + currentTransaction + " obj: " + this.stringifyObject(row, this.inspector));
        }

        public void update(long currentTransaction, Object row) throws IOException {
            this.out.println("update " + this.path + " currTxn: " + currentTransaction + " obj: " + this.stringifyObject(row, this.inspector));
        }

        public void delete(long currentTransaction, Object row) throws IOException {
            this.out.println("delete " + this.path + " currTxn: " + currentTransaction + " obj: " + row);
        }

        public void flush() throws IOException {
            this.out.println("flush " + this.path);
        }

        public void close(boolean abort) throws IOException {
            this.out.println("close " + this.path);
        }

        public SerDeStats getStats() {
            return null;
        }

        private void stringifyObject(StringBuilder buffer, Object obj, ObjectInspector inspector) throws IOException {
            if (inspector instanceof StructObjectInspector) {
                buffer.append("{ ");
                StructObjectInspector soi = (StructObjectInspector)inspector;
                boolean isFirst = true;
                Iterator i$ = soi.getAllStructFieldRefs().iterator();

                while(i$.hasNext()) {
                    StructField field = (StructField)i$.next();
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        buffer.append(", ");
                    }

                    buffer.append(field.getFieldName());
                    buffer.append(": ");
                    this.stringifyObject(buffer, soi.getStructFieldData(obj, field), field.getFieldObjectInspector());
                }

                buffer.append(" }");
            } else if (inspector instanceof PrimitiveObjectInspector) {
                PrimitiveObjectInspector poi = (PrimitiveObjectInspector)inspector;
                buffer.append(poi.getPrimitiveJavaObject(obj).toString());
            } else {
                buffer.append("*unknown*");
            }

        }

        private String stringifyObject(Object obj, ObjectInspector inspector) throws IOException {
            StringBuilder buffer = new StringBuilder();
            this.stringifyObject(buffer, obj, inspector);
            return buffer.toString();
        }
    }

    private static class OrcRecordWriter implements RecordWriter<NullWritable, OrcSerdeRow>, StatsProvidingRecordWriter {
        private Writer writer = null;
        private final Path path;
        private final WriterOptions options;
        private final SerDeStats stats;

        OrcRecordWriter(Path path, WriterOptions options) {
            this.path = path;
            this.options = options;
            this.stats = new SerDeStats();
        }

        public void write(NullWritable nullWritable, OrcSerdeRow row) throws IOException {
            if (this.writer == null) {
                this.options.inspector(row.getInspector());
                this.writer = OrcFile.createWriter(this.path, this.options);
            }

            this.writer.addRow(row.getRow());
        }

        public void write(Writable row) throws IOException {
            OrcSerdeRow serdeRow = (OrcSerdeRow)row;
            if (this.writer == null) {
                this.options.inspector(serdeRow.getInspector());
                this.writer = OrcFile.createWriter(this.path, this.options);
            }

            this.writer.addRow(serdeRow.getRow());
        }

        public void close(Reporter reporter) throws IOException {
            this.close(true);
        }

        public void close(boolean b) throws IOException {
            if (this.writer == null) {
                ObjectInspector inspector = ObjectInspectorFactory.getStandardStructObjectInspector(new ArrayList(), new ArrayList());
                this.options.inspector(inspector);
                this.writer = OrcFile.createWriter(this.path, this.options);
            }

            this.writer.close();
        }

        public SerDeStats getStats() {
            this.stats.setRawDataSize(this.writer.getRawDataSize());
            this.stats.setRowCount(this.writer.getNumberOfRows());
            return this.stats;
        }
    }
}
