package com.alibaba.datax.plugin.writer.emrfswriter;

/**
 * @author xiejiajun
 */
public interface Constant {

	String S3_FS_IMPL 												= "fs.s3.impl";
	String DEFAULT_S3_FS_IMPL										= "com.amazon.ws.emr.hadoop.fs.EmrFileSystem";
	String S3N_FS_IMPL 												= "fs.s3n.impl";
	String DEFAULT_S3N_FS_IMPL 										= "com.amazon.ws.emr.hadoop.fs.EmrFileSystem";
	String S3B_FS_IMPL 												= "fs.s3bfs.impl";
	String DEFAULT_S3B_FS_IMPL 										= "org.apache.hadoop.fs.s3.S3FileSystem";
	String S3_ABSTRACT_FS_IMPL 										= "fs.AbstractFileSystem.s3.impl";
	String DEFAULT_S3_ABSTRACT_FS_IMPL 								= "org.apache.hadoop.fs.s3.EMRFSDelegate";

	String EMRFS_CONSISTENT_RETRY_PERIOD_SECONDS 					= "fs.s3.consistent.retryPeriodSeconds";
	String DEFAULT_EMRFS_CONSISTENT_RETRY_PERIOD_SECONDS 			= "10";
	String EMRFS_CONSISTENT_RETRY_COUNT 							= "fs.s3.consistent.retryCount";
	String DEFAULT_EMRFS_CONSISTENT_RETRY_COUNT 					= "3";
	String EMRFS_CONSISTENT_ENABLED 								= "fs.s3.consistent";
	String EMRFS_CONSISTENT_METADATA_TABLENAME 						= "fs.s3.consistent.metadata.tableName";
	String DEFAULT_EMRFS_CONSISTENT_METADATA_TABLENAME 				= "EmrFSMetadata";
	String EMRFS_CONSISTENT_METADATA_ETAG_VERIFICATION_ENABLED 		= "fs.s3.consistent.metadata.etag.verification.enabled";


	String S3_REGION 												= "fs.s3.buckets.create.region";
	String S3N_FS_ENDPOINT 											= "fs.s3n.endpoint";
	String EMRFS_CONSISTENT_DDB_ENDPOINT 							= "fs.s3.consistent.dynamodb.endpoint";

	String S3_FS_ACCESS_KEY_ID 										= "fs.s3.awsAccessKeyId";
	String S3_FS_ACCESS_KEY_SECRET 									= "fs.s3.awsSecretAccessKey";

	String S3_FS_CONFIG_LOAD_ENABLED 								= "fs.s3.configuration.load.enabled";


	String DEFAULT_ENCODING 										= "UTF-8";

}
