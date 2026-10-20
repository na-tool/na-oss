package com.na.oss.config;

import com.na.oss.ali.IOssAliService;
import com.na.oss.ali.impl.OssAliServiceImpl;
import com.na.oss.hardDisk.IOssHardDiskService;
import com.na.oss.hardDisk.impl.OssHardDiskServiceImpl;
import com.na.oss.huawei.IOssHuaweiService;
import com.na.oss.huawei.impl.OssHuaweiServiceImpl;
import com.na.oss.qiniu.IOssQiNiuService;
import com.na.oss.qiniu.impl.OssQiNiuServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * na-oss 自动配置。
 * <p>
 * 通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 注册，使用方引入依赖后即可直接注入 OSS 服务，无需 {@code @ComponentScan("com.na")}。
 * </p>
 * 装配规则：
 * <ul>
 *     <li>{@link AutoOssConfig}：始终注册，对应配置前缀 {@code na.oss}；</li>
 *     <li>{@link IOssHardDiskService}：始终注册（本地存储无第三方 SDK 依赖）；</li>
 *     <li>阿里云/华为云/七牛云：仅当对应 SDK 在类路径中，且配置了对应 AccessKey 时注册。</li>
 * </ul>
 * <p>所有 Bean 均标注 {@link ConditionalOnMissingBean}，使用方可自行声明同类型的 Bean 覆盖默认实现。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(AutoOssConfig.class)
public class OssAutoConfiguration {

    /**
     * 本地磁盘存储服务。无第三方 SDK 依赖，默认始终注册。
     *
     * @param config OSS 配置
     * @return 本地磁盘存储服务
     */
    @Bean
    @ConditionalOnMissingBean
    public IOssHardDiskService ossHardDiskService(AutoOssConfig config) {
        return new OssHardDiskServiceImpl(config);
    }

    /**
     * 阿里云 OSS 配置：类路径存在阿里云 SDK 且配置了 aliAccessKey 时生效。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.aliyun.oss.OSS")
    @ConditionalOnProperty(prefix = "na.oss", name = "aliAccessKey")
    static class AliOssConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public IOssAliService ossAliService(AutoOssConfig config) {
            return new OssAliServiceImpl(config);
        }
    }

    /**
     * 华为云 OBS 配置：类路径存在华为云 SDK 且配置了 huaweiAccessKey 时生效。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.obs.services.ObsClient")
    @ConditionalOnProperty(prefix = "na.oss", name = "huaweiAccessKey")
    static class HuaweiOssConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public IOssHuaweiService ossHuaweiService(AutoOssConfig config) {
            return new OssHuaweiServiceImpl(config);
        }
    }

    /**
     * 七牛云配置：类路径存在七牛云 SDK 且配置了 qiNiuAccessKey 时生效。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.qiniu.storage.UploadManager")
    @ConditionalOnProperty(prefix = "na.oss", name = "qiNiuAccessKey")
    static class QiNiuOssConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public IOssQiNiuService ossQiNiuService(AutoOssConfig config) {
            return new OssQiNiuServiceImpl(config);
        }
    }
}
