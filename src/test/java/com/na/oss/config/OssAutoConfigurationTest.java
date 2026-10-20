package com.na.oss.config;

import com.na.oss.ali.IOssAliService;
import com.na.oss.hardDisk.IOssHardDiskService;
import com.na.oss.huawei.IOssHuaweiService;
import com.na.oss.qiniu.IOssQiNiuService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动配置测试。
 * <p>验证引入依赖后无需组件扫描即可获得配置与本地存储服务，
 * 且各云平台服务仅在配置了对应 AccessKey 时装配。</p>
 */
class OssAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OssAutoConfiguration.class));

    @Test
    void registersConfigAndHardDiskServiceByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AutoOssConfig.class);
            assertThat(context).hasSingleBean(IOssHardDiskService.class);
        });
    }

    @Test
    void doesNotRegisterCloudServicesWithoutAccessKey() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(IOssAliService.class);
            assertThat(context).doesNotHaveBean(IOssHuaweiService.class);
            assertThat(context).doesNotHaveBean(IOssQiNiuService.class);
        });
    }

    @Test
    void registersCloudServicesWhenAccessKeyConfigured() {
        contextRunner.withPropertyValues(
                        "na.oss.aliAccessKey=test-ak",
                        "na.oss.huaweiAccessKey=test-ak",
                        "na.oss.qiNiuAccessKey=test-ak")
                .run(context -> {
                    assertThat(context).hasSingleBean(IOssAliService.class);
                    assertThat(context).hasSingleBean(IOssHuaweiService.class);
                    assertThat(context).hasSingleBean(IOssQiNiuService.class);
                });
    }
}
