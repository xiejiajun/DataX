//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.amazon.ws.emr.hadoop.fs.util;

import com.amazon.ws.emr.hadoop.fs.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import com.amazon.ws.emr.hadoop.fs.shaded.com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public final class PlatformInfo {
    private final String extraInstanceDataJson;
    private final String emrJobFlowJson;
    private final String configEndpoint;
    private final Logger LOG;

    private PlatformInfo(String extraInstanceDataJson, String emrJobFlowJson, String configEndpoint) {
        this.LOG = LoggerFactory.getLogger(PlatformInfo.class);
        this.extraInstanceDataJson = extraInstanceDataJson;
        this.emrJobFlowJson = emrJobFlowJson;
        this.configEndpoint = configEndpoint;
    }

    public static PlatformInfo forDefaultResourceLocations() {
        return new PlatformInfo("/var/lib/instance-controller/extraInstanceData.json", "/var/lib/info/job-flow.json", "http://localhost:8321/configuration");
    }

    private String getClusterIdFromConfigurationEndpoint() throws IOException {
        return (String)((Map)(new ObjectMapper()).readValue(new URL(this.configEndpoint), Map.class)).get("clusterId");
    }

    private String getClusterIdFromEmrJobFlowJson() throws IOException {
        return (String)((Map)(new ObjectMapper()).readValue(new File(this.emrJobFlowJson), Map.class)).get("jobFlowId");
    }

    private String getClusterIdFromExtraInstanceDataJson() throws IOException {
        return (String)((Map)(new ObjectMapper()).readValue(new File(this.extraInstanceDataJson), Map.class)).get("jobFlowId");
    }

    public String getJobFlowId() {
        return null;
    }

    public static PlatformInfo.Builder builder() {
        return new PlatformInfo.Builder();
    }

    public static class Builder {
        private String extraInstanceDataJsonFile;
        private String emrJobFlowJsonFile;
        private String configEndpoint;

        public Builder() {
        }

        public PlatformInfo.Builder withExtraInstanceDataJsonFile(String extraInstanceDataJson) {
            this.extraInstanceDataJsonFile = extraInstanceDataJson;
            return this;
        }

        public PlatformInfo.Builder withEmrJobFlowJsonFile(String emrJobFlowJson) {
            this.emrJobFlowJsonFile = emrJobFlowJson;
            return this;
        }

        public PlatformInfo.Builder withConfigurationEndpoint(String configEndpoint) {
            this.configEndpoint = configEndpoint;
            return this;
        }

        public PlatformInfo build() {
            return new PlatformInfo((String)Optional.fromNullable(this.extraInstanceDataJsonFile).or("/var/lib/instance-controller/extraInstanceData.json"), (String)Optional.fromNullable(this.emrJobFlowJsonFile).or("/var/lib/info/job-flow.json"), (String)Optional.fromNullable(this.configEndpoint).or("http://localhost:8321/configuration"));
        }
    }
}
