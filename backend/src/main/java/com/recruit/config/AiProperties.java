package com.recruit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 多模型配置（application.yml 的 ai: 段）
 * providers 下每个提供方含 key/url/model 三项（OpenAI 兼容接口），
 * default-provider 指定默认通道；某通道 key 缺失时调用自动降级 mock
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * 默认提供方名称，对应 providers 里的 key（deepseek/glm）
     */
    private String defaultProvider = "deepseek";

    /**
     * 各提供方配置：名称 -> {key, url, model}
     */
    private Map<String, Provider> providers = new LinkedHashMap<>();

    public static class Provider {

        private String key;

        private String url;

        private String model;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }
}
