package com.alibaba.datax.plugin.writer.emrfswriter;

import com.alibaba.datax.common.spi.ErrorCode;

/**
 * Created by shf on 15/10/8.
 */
public enum EmrfsWriterErrorCode implements ErrorCode {

    /**
     * Emrfs错误码定义
     */
    REQUIRED_VALUE("EmrfsWriter-01", "您缺失了必须填写的参数值."),
    ILLEGAL_VALUE("EmrfsWriter-02", "您填写的参数值不合法."),
    Write_FILE_IO_ERROR("EmrfsWriter-03", "您配置的文件在写入时出现IO异常."),
    CONNECT_EMRFS_IO_ERROR("EmrfsWriter-04", "与S3建立连接时出现IO异常."),
    COLUMN_REQUIRED_VALUE("EmrfsWriter-04", "您column配置中缺失了必须填写的参数值."),
    EMRFS_RENAME_FILE_ERROR("EmrfsWriter-06", "将文件移动到配置路径失败.");

    private final String code;
    private final String description;

    EmrfsWriterErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Description:[%s].", this.code,
                this.description);
    }

}
