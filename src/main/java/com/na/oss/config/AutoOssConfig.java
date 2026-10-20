package com.na.oss.config;

import com.na.oss.utils.OssFileUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * OSS 自动配置属性。
 * <p>
 * 对应配置前缀 {@code na.oss}，包含本地磁盘、七牛云、阿里云、华为云四类存储的
 * 密钥、Bucket、域名、大小限制与文件类型白名单等配置。
 * 各平台未单独配置域名/大小时，会自动回退到全局 {@code domain}/{@code maxSize}。
 * </p>
 * <p>由 {@link OssAutoConfiguration} 通过 {@code @EnableConfigurationProperties} 注册，
 * 使用方无需额外组件扫描。</p>
 */
@Data
@ConfigurationProperties(prefix = "na.oss")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoOssConfig {

    /**
     * 全局最大文件大小限制，默认 10MB
     * 单位：字节
     */
    @Builder.Default
    private Long maxSize = 10485760L;

    /**
     * 全局允许文件类型白名单，默认 jpg/jpeg/gif/png
     * <p>各平台未单独配置白名单时使用该值。</p>
     */
    @Builder.Default
    private Set<String> suffixNameWhites = new HashSet<>(Arrays.asList(OssFileUtil.FILE_TYPE));

    /**
     * 全局自定义访问域名
     */
    private String domain;


    /**
     * 硬盘配置开始   -----------------------------------------------------------------------------------
     */

    /**
     * 本地存储根路径【必填】，例如 {@code D:/test/}，结尾必须带 /
     */
    private String localPath;

    /**
     * 本地上传文件大小限制（与 {@link #maxSize} 二选一）
     */
    private Long localMaxSize;

    /**
     * 本地允许的文件后缀（与 {@link #suffixNameWhites} 二选一）
     */
    @Builder.Default
    private Set<String> localWhite = new HashSet<>();

    /**
     * 获取本地允许的文件后缀，未配置时回退到全局白名单。
     *
     * @return 文件后缀集合
     */
    public Set<String> getLocalWhite() {
        return CollectionUtils.isEmpty(localWhite) ? defaultWhites() : localWhite;
    }

    /**
     * 硬盘配置结束   -----------------------------------------------------------------------------------
     */

    /**
     * 七牛云配置开始  -----------------------------------------------------------------------------------
     */
    /**
     * 七牛云 AccessKey【必填】
     */
    private String qiNiuAccessKey;

    /**
     * 七牛云 SecretKey【必填】
     */
    private String qiNiuSecretKey;

    /**
     * 七牛云空间名称【必填】
     */
    private String qiNiuBucketName;

    /**
     * 七牛云自定义域名，与全局 {@link #domain} 二选一
     */
    private String qiNiuDomain;

    /**
     * 获取七牛云访问域名，未配置时回退到全局域名。
     *
     * @return 七牛云访问域名
     */
    public String getQiNiuDomain() {
        return StringUtils.isEmpty(qiNiuDomain) ? domain : qiNiuDomain;
    }

    /**
     * 七牛云上传文件大小限制，与全局 {@link #maxSize} 二选一
     */
    private Long qiNiuMaxSize;

    /**
     * 七牛云允许文件类型白名单，与 {@link #suffixNameWhites} 二选一
     */
    @Builder.Default
    private Set<String> qiNiuWhite = new HashSet<>();

    /**
     * 获取七牛云允许的文件后缀，未配置时回退到全局白名单。
     *
     * @return 文件后缀集合
     */
    public Set<String> getQiNiuWhite() {
        return CollectionUtils.isEmpty(qiNiuWhite) ? defaultWhites() : qiNiuWhite;
    }

    /**
     * 上传前是否自动删除同路径的旧文件（七牛云空间默认同名覆盖，按需开启）
     */
    @Builder.Default
    private Boolean qiNiuAutoDelete = false;

    /**
     * 七牛云配置结束   -----------------------------------------------------------------------------------
     */


    /**
     * 阿里云配置开始   -----------------------------------------------------------------------------------
     */

    /**
     * 阿里云 AccessKey【必填】
     */
    private String aliAccessKey;

    /**
     * 阿里云 SecretKey【必填】
     */
    private String aliSecretKey;

    /**
     * 阿里云地域节点【必填】，例如 {@code oss-cn-beijing.aliyuncs.com}
     */
    private String aliEndpoint;

    /**
     * 阿里云空间名称【必填】
     */
    private String aliBucketName;

    /**
     * 阿里云自定义域名，与全局 {@link #domain} 二选一
     */
    private String aliDomain;

    /**
     * 获取阿里云访问域名，未配置时回退到全局域名。
     *
     * @return 阿里云访问域名
     */
    public String getAliDomain() {
        return StringUtils.isEmpty(aliDomain) ? domain : aliDomain;
    }

    /**
     * 阿里云上传文件大小限制，与全局 {@link #maxSize} 二选一
     */
    private Long aliMaxSize;

    /**
     * 阿里云允许文件类型白名单，与 {@link #suffixNameWhites} 二选一
     */
    @Builder.Default
    private Set<String> aliWhite = new HashSet<>();

    /**
     * 获取阿里云允许的文件后缀，未配置时回退到全局白名单。
     *
     * @return 文件后缀集合
     */
    public Set<String> getAliWhite() {
        return CollectionUtils.isEmpty(aliWhite) ? defaultWhites() : aliWhite;
    }

    /**
     * 阿里云配置结束   -----------------------------------------------------------------------------------
     */

    /**
     * 华为云配置开始   -----------------------------------------------------------------------------------
     */

    /**
     * 华为云 AccessKey【必填】
     */
    private String huaweiAccessKey;

    /**
     * 华为云 SecretKey【必填】
     */
    private String huaweiSecretKey;

    /**
     * 华为云地域节点【必填】，例如 {@code obs.cn-north-4.myhuaweicloud.com}
     */
    private String huaweiEndpoint;

    /**
     * 华为云空间名称【必填】
     */
    private String huaweiBucketName;

    /**
     * 华为云区域名称，默认 {@code cn-north-4}
     */
    @Builder.Default
    private String huaweiArea = "cn-north-4";

    /**
     * 华为云自定义域名，与全局 {@link #domain} 二选一
     */
    private String huaweiDomain;

    /**
     * 获取华为云访问域名，未配置时回退到全局域名。
     *
     * @return 华为云访问域名
     */
    public String getHuaweiDomain() {
        return StringUtils.isEmpty(huaweiDomain) ? domain : huaweiDomain;
    }

    /**
     * 华为云上传文件大小限制，与全局 {@link #maxSize} 二选一
     */
    private Long huaweiMaxSize;

    /**
     * 华为云允许文件类型白名单，与 {@link #suffixNameWhites} 二选一
     */
    @Builder.Default
    private Set<String> huaweiWhite = new HashSet<>();

    /**
     * 获取华为云允许的文件后缀，未配置时回退到全局白名单。
     *
     * @return 文件后缀集合
     */
    public Set<String> getHuaweiWhite() {
        return CollectionUtils.isEmpty(huaweiWhite) ? defaultWhites() : huaweiWhite;
    }

    /**
     * 华为云配置结束   -----------------------------------------------------------------------------------
     */

    /**
     * 获取全局白名单；若全局白名单也被配置为空，则使用默认图片后缀兜底。
     *
     * @return 文件后缀集合
     */
    private Set<String> defaultWhites() {
        if (CollectionUtils.isEmpty(suffixNameWhites)) {
            return new HashSet<>(Arrays.asList(OssFileUtil.FILE_TYPE));
        }
        return suffixNameWhites;
    }
}
