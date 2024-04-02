package com.cn.tvn.awscopy.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "s3sync")
public class S3SyncProperties {
    private Map<String, String> prefixesMap;

    public Map<String, String> getPrefixesMap() {
        return prefixesMap;
    }

    public void setPrefixesMap(Map<String, String> prefixesMap) {
        this.prefixesMap = prefixesMap;
    }

    public String getDestPrefix(String sourcePrefix) {
        return prefixesMap.get(sourcePrefix);
    }
}
