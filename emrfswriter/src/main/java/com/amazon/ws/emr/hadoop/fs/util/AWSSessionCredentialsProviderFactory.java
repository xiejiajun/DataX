package com.amazon.ws.emr.hadoop.fs.util;

import com.amazon.ws.emr.hadoop.fs.shaded.com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider.Builder;
import com.amazon.ws.emr.hadoop.fs.shaded.com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazon.ws.emr.hadoop.fs.shaded.com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazon.ws.emr.hadoop.fs.shaded.com.google.common.annotations.VisibleForTesting;
import com.amazon.ws.emr.hadoop.fs.shaded.com.google.common.cache.Cache;
import com.amazon.ws.emr.hadoop.fs.shaded.com.google.common.cache.CacheBuilder;
import com.amazonaws.auth.AWSSessionCredentialsProvider;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;

public final class AWSSessionCredentialsProviderFactory {
    private static final AWSSecurityTokenService STS_CLIENT = buildSTSClient();
    private static final Cache<String, AWSSessionCredentialsProvider> PROVIDERS_CACHE = CacheBuilder.newBuilder().maximumSize(100L).initialCapacity(10).concurrencyLevel(10).build();

    private AWSSessionCredentialsProviderFactory() {
    }

    @Nullable
    public static AWSSessionCredentialsProvider getCredentialsProviderForRoleArn(String roleArn) {
        if (roleArn == null) {
            return null;
        } else {
            AWSSessionCredentialsProvider cachedProvider = null;

            try {
                cachedProvider = (AWSSessionCredentialsProvider)PROVIDERS_CACHE.get(roleArn, () -> {
                    return createCredentialsProviderForRoleArn(roleArn);
                });
            } catch (ExecutionException var3) {
            }

            return cachedProvider;
        }
    }

    private static AWSSessionCredentialsProvider createCredentialsProviderForRoleArn(String roleArn) {
        return (new Builder(roleArn, "EmrFS-Session")).withStsClient(STS_CLIENT).build();
    }

    @VisibleForTesting
    static AWSSecurityTokenService buildSTSClient() {
        return AWSSecurityTokenServiceClientBuilder.defaultClient();
    }
}
