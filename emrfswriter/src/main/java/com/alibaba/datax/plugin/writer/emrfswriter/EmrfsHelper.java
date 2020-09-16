package com.alibaba.datax.plugin.writer.emrfswriter;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.plugin.TaskPluginCollector;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.*;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat;
import org.apache.hadoop.hive.ql.io.orc.OrcSerde;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.mapred.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.alibaba.datax.plugin.writer.emrfswriter.Constant.*;

/**
 * @author xiejiajun & liuyanfeng
 */
public  class EmrfsHelper {
    private static final Logger LOG = LoggerFactory.getLogger(EmrfsWriter.Job.class);
    private FileSystem fileSystem = null;
    private JobConf conf = null;
    private org.apache.hadoop.conf.Configuration emrfsConf = null;
    private static final String S3_DEFAULTFS = "fs.defaultFS";

    /**
     * @author liuyanfeng
     * @param defaultFS
     * @param taskConfig
     */
    void getFileSystem(String defaultFS, Configuration taskConfig){
        emrfsConf = new org.apache.hadoop.conf.Configuration();

        Configuration emrfsParams = taskConfig.getConfiguration(Key.EMRFS_CONFIG);
        JSONObject emrfsParamsAsJsonObject = JSON.parseObject(taskConfig.getString(Key.EMRFS_CONFIG));
        if (null != emrfsParams) {
            Set<String> paramKeys = emrfsParams.getKeys();
            for (String paramKey: paramKeys) {
                emrfsConf.set(paramKey, emrfsParamsAsJsonObject.getString(paramKey));
            }
        }
        emrfsConf.set(S3_DEFAULTFS, defaultFS);

        this.rewriteEmrfsConfig();

        conf = new JobConf(emrfsConf);
        try {
            fileSystem = FileSystem.get(conf);
        } catch (IOException e) {
            String message = String.format("获取FileSystem时发生网络IO异常,请检查您的网络是否正常!S3文件系统：[%s]",
                    "message:defaultFS =" + defaultFS);
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
        }catch (Exception e) {
            String message = String.format("获取FileSystem失败,请检查S3地址是否正确: [%s]",
                    "message:defaultFS =" + defaultFS);
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
        }

        if(null == fileSystem || null == conf){
            String message = String.format("获取FileSystem失败,请检查S3地址是否正确: [%s]",
                    "message:defaultFS =" + defaultFS);
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, message);
        }
    }

    /**
     * @author liuyanfeng
     * 校验并重写EMRFS配置
     */
    private void rewriteEmrfsConfig() {
        emrfsConf.set(Constant.S3_FS_IMPL,Constant.DEFAULT_S3_FS_IMPL);
        emrfsConf.set(Constant.S3N_FS_IMPL,Constant.DEFAULT_S3N_FS_IMPL);
        emrfsConf.set(Constant.S3B_FS_IMPL,Constant.DEFAULT_S3B_FS_IMPL);
        emrfsConf.set(Constant.S3_ABSTRACT_FS_IMPL,Constant.DEFAULT_S3_ABSTRACT_FS_IMPL);
        emrfsConf.set(Constant.S3_FS_CONFIG_LOAD_ENABLED,"false");

        // 默认开启一致性视图
        if (emrfsConf.get(EMRFS_CONSISTENT_ENABLED) == null){
            emrfsConf.set(EMRFS_CONSISTENT_ENABLED,"true");
            emrfsConf.set(EMRFS_CONSISTENT_RETRY_PERIOD_SECONDS,DEFAULT_EMRFS_CONSISTENT_RETRY_PERIOD_SECONDS);
            emrfsConf.set(EMRFS_CONSISTENT_RETRY_COUNT,DEFAULT_EMRFS_CONSISTENT_RETRY_COUNT);
            emrfsConf.set(EMRFS_CONSISTENT_METADATA_TABLENAME,DEFAULT_EMRFS_CONSISTENT_METADATA_TABLENAME);
            emrfsConf.set(EMRFS_CONSISTENT_METADATA_ETAG_VERIFICATION_ENABLED,"false");
        }
        if (StringUtils.isBlank(emrfsConf.get(S3_REGION))){
            emrfsConf.unset(S3_REGION);
        }
        if (StringUtils.isBlank(emrfsConf.get(S3N_FS_ENDPOINT))){
            emrfsConf.unset(S3N_FS_ENDPOINT);
        }
        if (StringUtils.isBlank(emrfsConf.get(EMRFS_CONSISTENT_DDB_ENDPOINT))){
            emrfsConf.unset(EMRFS_CONSISTENT_DDB_ENDPOINT);
        }
        if (StringUtils.isBlank(emrfsConf.get(S3_FS_ACCESS_KEY_ID))){
            emrfsConf.unset(S3_FS_ACCESS_KEY_ID);
        }
        if (StringUtils.isBlank(emrfsConf.get(S3_FS_ACCESS_KEY_SECRET))){
            emrfsConf.unset(S3_FS_ACCESS_KEY_SECRET);
        }

    }


    /**
     * 获取指定目录先的文件列表
     * @param dir
     * @return
     * 拿到的是文件全路径，
     * eg：s3://bucket1/user/hive/warehouse/writer.db/text/test.textfile
     */
    String[] emrfsDirList(String dir){
        Path path = new Path(dir);
        String[] files = null;
        try {
            FileStatus[] status = fileSystem.listStatus(path);
            files = new String[status.length];
            for(int i=0;i<status.length;i++){
                files[i] = status[i].getPath().toString();
            }
        } catch (IOException e) {
            String message = String.format("获取目录[%s]文件列表时发生网络IO异常,请检查您的网络是否正常！", dir);
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
        }
        return files;
    }

    /**
     * 获取以fileName__ 开头的文件列表
     * @param dir
     * @param fileName
     * @return
     */
    Path[] emrfsDirList(String dir, String fileName){
        Path path = new Path(dir);
        Path[] files = null;
        String filterFileName = fileName + "__*";
        try {
            PathFilter pathFilter = new GlobFilter(filterFileName);
            FileStatus[] status = fileSystem.listStatus(path,pathFilter);
            files = new Path[status.length];
            for(int i=0;i<status.length;i++){
                files[i] = status[i].getPath();
            }
        } catch (IOException e) {
            String message = String.format("获取目录[%s]下文件名以[%s]开头的文件列表时发生网络IO异常,请检查您的网络是否正常！",
                    dir,fileName);
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
        }
        return files;
    }

    boolean isPathexists(String filePath) {
        Path path = new Path(filePath);
        boolean exist = false;
        try {
            exist = fileSystem.exists(path);
        } catch (IOException e) {
            String message = String.format("判断文件路径[%s]是否存在时发生网络IO异常,请检查您的网络是否正常！",
                    "message:filePath =" + filePath);
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
        }
        return exist;
    }

    boolean isPathDir(String filePath) {
        Path path = new Path(filePath);
        boolean isDir = false;
        try {
            isDir = fileSystem.isDirectory(path);
        } catch (IOException e) {
            String message = String.format("判断路径[%s]是否是目录时发生网络IO异常,请检查您的网络是否正常！", filePath);
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
        }
        return isDir;
    }

    void deleteFiles(Path[] paths){
        for (Path path : paths) {
            LOG.info(String.format("delete file [%s].", path.toString()));
            try {
                fileSystem.delete(path, true);
            } catch (IOException e) {
                String message = String.format("删除文件[%s]时发生IO异常,请检查您的网络是否正常！",
                        path.toString());
                LOG.error(message);
                throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
            }
        }
    }

    private void deleteDir(Path path){
        LOG.info(String.format("start delete tmp dir [%s] .",path.toString()));
        try {
            if(isPathexists(path.toString())) {
                fileSystem.delete(path, true);
            }
        } catch (Exception e) {
            String message = String.format("删除临时目录[%s]时发生IO异常,请检查您的网络是否正常！", path.toString());
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
        }
        LOG.info(String.format("finish delete tmp dir [%s] .",path.toString()));
    }

    void renameFile(HashSet<String> tmpFiles, HashSet<String> endFiles){
        Path tmpFilesParent = null;
        if(tmpFiles.size() != endFiles.size()){
            String message = String.format("临时目录下文件名个数与目标文件名个数不一致!");
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.EMRFS_RENAME_FILE_ERROR, message);
        }else{
            try{
                for (Iterator it1=tmpFiles.iterator(),it2=endFiles.iterator();it1.hasNext()&&it2.hasNext();){
                    String srcFile = it1.next().toString();
                    String dstFile = it2.next().toString();
                    Path srcFilePah = new Path(srcFile);
                    Path dstFilePah = new Path(dstFile);
                    if(tmpFilesParent == null){
                        tmpFilesParent = srcFilePah.getParent();
                    }
                    LOG.info(String.format("start rename file [%s] to file [%s].", srcFile,dstFile));
                    boolean renameTag = false;
                    long fileLen = fileSystem.getFileStatus(srcFilePah).getLen();
                    if(fileLen>0){
                        renameTag = fileSystem.rename(srcFilePah,dstFilePah);
                        if(!renameTag){
                            String message = String.format("重命名文件[%s]失败,请检查您的网络是否正常！", srcFile);
                            LOG.error(message);
                            throw DataXException.asDataXException(EmrfsWriterErrorCode.EMRFS_RENAME_FILE_ERROR, message);
                        }
                        LOG.info(String.format("finish rename file [%s] to file [%s].", srcFile,dstFile));
                    }else{
                        LOG.info(String.format("文件［%s］内容为空,请检查写入是否正常！", srcFile));
                    }
                }
            }catch (Exception e) {
                String message = String.format("重命名文件时发生异常,请检查您的网络是否正常！");
                LOG.error(message);
                throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
            }finally {
                deleteDir(tmpFilesParent);
            }
        }
    }

    /**
     * 释放资源
     */
    void closeFileSystem(){
        try {
            fileSystem.close();
        } catch (IOException e) {
            String message = String.format("关闭FileSystem时发生IO异常,请检查您的网络是否正常！");
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.CONNECT_EMRFS_IO_ERROR, e);
        }
    }


    /**
     * 处理txt文件
     * @param path
     * @return
     */
    public  FSDataOutputStream getOutputStream(String path){
        Path storePath = new Path(path);
        FSDataOutputStream fSDataOutputStream = null;
        try {
            fSDataOutputStream = fileSystem.create(storePath);
        } catch (IOException e) {
            String message = String.format("Create an FSDataOutputStream at the indicated Path[%s] failed: [%s]",
                    "message:path =" + path);
            LOG.error(message);
            throw DataXException.asDataXException(EmrfsWriterErrorCode.Write_FILE_IO_ERROR, e);
        }
        return fSDataOutputStream;
    }

    /**
     * 写textfile类型文件
     * @param lineReceiver
     * @param config
     * @param fileName
     * @param taskPluginCollector
     */
    void textFileStartWrite(RecordReceiver lineReceiver, Configuration config, String fileName,
                            TaskPluginCollector taskPluginCollector){
        char fieldDelimiter = config.getChar(Key.FIELD_DELIMITER);
        List<Configuration>  columns = config.getListConfiguration(Key.COLUMN);
        String compress = config.getString(Key.COMPRESS,null);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        String attempt = "attempt_"+dateFormat.format(new Date())+"_0001_m_000000_0";
        Path outputPath = new Path(fileName);
        //todo 需要进一步确定TASK_ATTEMPT_ID
        conf.set(JobContext.TASK_ATTEMPT_ID, attempt);
        FileOutputFormat outFormat = new TextOutputFormat();
        outFormat.setOutputPath(conf, outputPath);
        outFormat.setWorkOutputPath(conf, outputPath);
        if(null != compress) {
            Class<? extends CompressionCodec> codecClass = getCompressCodec(compress);
            if (null != codecClass) {
                outFormat.setOutputCompressorClass(conf, codecClass);
            }
        }
        try {
            RecordWriter writer = outFormat.getRecordWriter(fileSystem, conf, outputPath.toString(), Reporter.NULL);
            Record record = null;
            while ((record = lineReceiver.getFromReader()) != null) {
                MutablePair<Text, Boolean> transportResult = transportOneRecord(record, fieldDelimiter, columns, taskPluginCollector);
                if (!transportResult.getRight()) {
                    writer.write(NullWritable.get(),transportResult.getLeft());
                }
            }
            writer.close(Reporter.NULL);
        } catch (Exception e) {
            String message = String.format("写文件文件[%s]时发生IO异常,请检查您的网络是否正常！", fileName);
            LOG.error(message);
            Path path = new Path(fileName);
            deleteDir(path.getParent());
            throw DataXException.asDataXException(EmrfsWriterErrorCode.Write_FILE_IO_ERROR, e);
        }
    }

    private static MutablePair<Text, Boolean> transportOneRecord(
            Record record, char fieldDelimiter, List<Configuration> columnsConfiguration, TaskPluginCollector taskPluginCollector) {
        MutablePair<List<Object>, Boolean> transportResultList =  transportOneRecord(record,columnsConfiguration,taskPluginCollector);
        //保存<转换后的数据,是否是脏数据>
        MutablePair<Text, Boolean> transportResult = new MutablePair<Text, Boolean>();
        transportResult.setRight(false);
        if(null != transportResultList){
            Text recordResult = new Text(StringUtils.join(transportResultList.getLeft(), fieldDelimiter));
            transportResult.setRight(transportResultList.getRight());
            transportResult.setLeft(recordResult);
        }
        return transportResult;
    }

    private Class<? extends CompressionCodec>  getCompressCodec(String compress){
        Class<? extends CompressionCodec> codecClass = null;
        if(null == compress){
            codecClass = null;
        }else if("GZIP".equalsIgnoreCase(compress)){
            codecClass = org.apache.hadoop.io.compress.GzipCodec.class;
        }else if ("BZIP2".equalsIgnoreCase(compress)) {
            codecClass = org.apache.hadoop.io.compress.BZip2Codec.class;
        }else if("SNAPPY".equalsIgnoreCase(compress)){
            //todo 等需求明确后支持 需要用户安装SnappyCodec
            codecClass = org.apache.hadoop.io.compress.SnappyCodec.class;
            // org.apache.hadoop.hive.ql.io.orc.ZlibCodec.class  not public
            //codecClass = org.apache.hadoop.hive.ql.io.orc.ZlibCodec.class;
        }else {
            throw DataXException.asDataXException(EmrfsWriterErrorCode.ILLEGAL_VALUE,
                    String.format("目前不支持您配置的 compress 模式 : [%s]", compress));
        }
        return codecClass;
    }

    /**
     * 写orcfile类型文件
     * @param lineReceiver
     * @param config
     * @param fileName
     * @param taskPluginCollector
     */
    void orcFileStartWrite(RecordReceiver lineReceiver, Configuration config, String fileName,
                           TaskPluginCollector taskPluginCollector){
        List<Configuration>  columns = config.getListConfiguration(Key.COLUMN);
        String compress = config.getString(Key.COMPRESS, null);
        List<String> columnNames = getColumnNames(columns);
        List<ObjectInspector> columnTypeInspectors = getColumnTypeInspectors(columns);
        StructObjectInspector inspector = (StructObjectInspector)ObjectInspectorFactory
                .getStandardStructObjectInspector(columnNames, columnTypeInspectors);

        OrcSerde orcSerde = new OrcSerde();

        FileOutputFormat outFormat = new OrcOutputFormat();
        if(!"NONE".equalsIgnoreCase(compress) && null != compress ) {
            Class<? extends CompressionCodec> codecClass = getCompressCodec(compress);
            if (null != codecClass) {
                outFormat.setOutputCompressorClass(conf, codecClass);
            }
        }
        try {
            RecordWriter writer = outFormat.getRecordWriter(fileSystem, conf, fileName, Reporter.NULL);
            Record record = null;
            while ((record = lineReceiver.getFromReader()) != null) {
                MutablePair<List<Object>, Boolean> transportResult =  transportOneRecord(record,columns,taskPluginCollector);
                if (!transportResult.getRight()) {
                    writer.write(NullWritable.get(), orcSerde.serialize(transportResult.getLeft(), inspector));
                }
            }
            writer.close(Reporter.NULL);
        } catch (Exception e) {
            String message = String.format("写文件文件[%s]时发生IO异常,请检查您的网络是否正常！", fileName);
            LOG.error(message);
            Path path = new Path(fileName);
            deleteDir(path.getParent());
            throw DataXException.asDataXException(EmrfsWriterErrorCode.Write_FILE_IO_ERROR, e);
        }
    }

    private List<String> getColumnNames(List<Configuration> columns){
        List<String> columnNames = Lists.newArrayList();
        for (Configuration eachColumnConf : columns) {
            columnNames.add(eachColumnConf.getString(Key.NAME));
        }
        return columnNames;
    }

    /**
     * 根据writer配置的字段类型，构建inspector
     * @param columns
     * @return
     */
    private List<ObjectInspector>  getColumnTypeInspectors(List<Configuration> columns){
        List<ObjectInspector>  columnTypeInspectors = Lists.newArrayList();
        for (Configuration eachColumnConf : columns) {
            SupportHiveDataType columnType = SupportHiveDataType.valueOf(eachColumnConf.getString(Key.TYPE).toUpperCase());
            ObjectInspector objectInspector = null;
            switch (columnType) {
                case TINYINT:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(Byte.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                case SMALLINT:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(Short.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                case INT:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(Integer.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                case BIGINT:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(Long.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                case FLOAT:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(Float.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                case DOUBLE:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(Double.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                case TIMESTAMP:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(java.sql.Timestamp.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                case DATE:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(java.sql.Date.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                case STRING:
                case VARCHAR:
                case CHAR:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(String.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                case BOOLEAN:
                    objectInspector = ObjectInspectorFactory.getReflectionObjectInspector(Boolean.class, ObjectInspectorFactory.ObjectInspectorOptions.JAVA);
                    break;
                default:
                    throw DataXException
                            .asDataXException(
                                    EmrfsWriterErrorCode.ILLEGAL_VALUE,
                                    String.format(
                                            "您的配置文件中的列配置信息有误. 因为DataX 不支持数据库写入这种字段类型. 字段名:[%s], 字段类型:[%d]. 请修改表中该字段的类型或者不同步该字段.",
                                            eachColumnConf.getString(Key.NAME),
                                            eachColumnConf.getString(Key.TYPE)));
            }

            columnTypeInspectors.add(objectInspector);
        }
        return columnTypeInspectors;
    }

    public OrcSerde getOrcSerde(Configuration config){
        String fieldDelimiter = config.getString(Key.FIELD_DELIMITER);
        String compress = config.getString(Key.COMPRESS);
        String encoding = config.getString(Key.ENCODING);

        OrcSerde orcSerde = new OrcSerde();
        Properties properties = new Properties();
        properties.setProperty("orc.bloom.filter.columns", fieldDelimiter);
        properties.setProperty("orc.compress", compress);
        properties.setProperty("orc.encoding.strategy", encoding);

        orcSerde.initialize(conf, properties);
        return orcSerde;
    }

    private static MutablePair<List<Object>, Boolean> transportOneRecord(
            Record record, List<Configuration> columnsConfiguration,
            TaskPluginCollector taskPluginCollector){

        MutablePair<List<Object>, Boolean> transportResult = new MutablePair<List<Object>, Boolean>();
        transportResult.setRight(false);
        List<Object> recordList = Lists.newArrayList();
        int recordLength = record.getColumnNumber();
        if (0 != recordLength) {
            Column column;
            for (int i = 0; i < recordLength; i++) {
                column = record.getColumn(i);
                //todo as method
                if (null != column.getRawData()) {
                    String rowData = column.getRawData().toString();
                    SupportHiveDataType columnType = SupportHiveDataType.valueOf(
                            columnsConfiguration.get(i).getString(Key.TYPE).toUpperCase());
                    //根据writer端类型配置做类型转换
                    try {
                        switch (columnType) {
                            case TINYINT:
                                recordList.add(Byte.valueOf(rowData));
                                break;
                            case SMALLINT:
                                recordList.add(Short.valueOf(rowData));
                                break;
                            case INT:
                                recordList.add(Integer.valueOf(rowData));
                                break;
                            case BIGINT:
                                recordList.add(column.asLong());
                                break;
                            case FLOAT:
                                recordList.add(Float.valueOf(rowData));
                                break;
                            case DOUBLE:
                                recordList.add(column.asDouble());
                                break;
                            case STRING:
                            case VARCHAR:
                            case CHAR:
                                recordList.add(column.asString());
                                break;
                            case BOOLEAN:
                                recordList.add(column.asBoolean());
                                break;
                            case DATE:
                                recordList.add(new java.sql.Date(column.asDate().getTime()));
                                break;
                            case TIMESTAMP:
                                recordList.add(new java.sql.Timestamp(column.asDate().getTime()));
                                break;
                            default:
                                throw DataXException
                                        .asDataXException(
                                                EmrfsWriterErrorCode.ILLEGAL_VALUE,
                                                String.format(
                                                        "您的配置文件中的列配置信息有误. 因为DataX 不支持数据库写入这种字段类型. 字段名:[%s], 字段类型:[%d]. 请修改表中该字段的类型或者不同步该字段.",
                                                        columnsConfiguration.get(i).getString(Key.NAME),
                                                        columnsConfiguration.get(i).getString(Key.TYPE)));
                        }
                    } catch (Exception e) {
                        // warn: 此处认为脏数据
                        String message = String.format(
                                "字段类型转换错误：你目标字段为[%s]类型，实际字段值为[%s].",
                                columnsConfiguration.get(i).getString(Key.TYPE), column.getRawData().toString());
                        taskPluginCollector.collectDirtyRecord(record, message);
                        transportResult.setRight(true);
                        break;
                    }
                }else {
                    // warn: it's all ok if nullFormat is null
                    recordList.add(null);
                }
            }
        }
        transportResult.setLeft(recordList);
        return transportResult;
    }
}
