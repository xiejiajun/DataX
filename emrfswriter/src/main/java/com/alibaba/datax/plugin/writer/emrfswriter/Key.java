package com.alibaba.datax.plugin.writer.emrfswriter;

/**
 * @author xiejiajun
 */
public interface Key {
    /**
     * 必填参数
     */
    String PATH = "path";

    /**
     * 必填参数
     */
    String S3_BUCKET = "bucket";

    /**
     * 必填参数
     */
    String FILE_TYPE = "fileType";

    /**
     * 必填参数
     */
    String FILE_NAME = "fileName";

    /**
     * 必填参数
     */
    String COLUMN = "column";
    String NAME = "name";
    String TYPE = "type";

    /**
     *
     */
    String WRITE_MODE = "writeMode";
    /**
     *
     */
    String FIELD_DELIMITER = "fieldDelimiter";
    // not must, default UTF-8
    /**
     * 非必填，默认UTF-8
     */
    String ENCODING = "encoding";
    /**
     * 非必填，默认不压缩
     */
    String COMPRESS = "compress";

    /**
     * Emrfs配置
     */
    String EMRFS_CONFIG = "emrfsConfig";
}
