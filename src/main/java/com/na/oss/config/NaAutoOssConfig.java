package com.na.oss.config;

import com.na.oss.utils.NaOssFileUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Component
@ConfigurationProperties(prefix = "na.oss")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaAutoOssConfig {
    /**
     * 全局最大文件大小限制, 默认 10M
     * 单位：字节
     */
    @Builder.Default
    private Long maxSize = 10485760L;

    /**
     * 全局允许文件类型白名单
     * .jpg", ".jpeg", ".gif", ".png
     */
    @Builder.Default
    private Set<String> suffixNameWhites = Arrays.stream(NaOssFileUtil.FILE_TYPE)
            .collect(Collectors.toSet());

    /**
     * 全局自定义域名
     */
    private String domain;


    /**
     * 硬盘配置开始   -----------------------------------------------------------------------------------
     */

    /**
     * 本地存储路径【必填】 例如 D:/test/ 结尾必须带/
     */
    private String localPath;

    /**
     * 本地上传文件大小限制（与 maxSize 二选一）
     */
    private Long localMaxSize;

    /**
     * 本地允许的文件后缀（与 fileWhiteSuffixNames 二选一）
     */
    @Builder.Default
    private Set<String> localWhite = new HashSet<>();

    public Set<String> getLocalWhite() {
        return CollectionUtils.isEmpty(localWhite) ? suffixNameWhites : localWhite;
    }

    /**
     * 硬盘配置结束   -----------------------------------------------------------------------------------
     */

    /**
     * 七牛云配置开始  -----------------------------------------------------------------------------------
     */
    /**
     * 七牛云密钥对  【必填】
     */
//    @Builder.Default
    private String qiNiuAccessKey;
//    @Builder.Default
    private String qiNiuSecretKey;
    /**
     * 空间名称  【必填】
     */
    private String qiNiuBucketName;
    /**
     * 七牛云自定义域名  和【domain】二选一
     */
    private String qiNiuDomain;
    public String getQiNiuDomain() {
        return StringUtils.isEmpty(qiNiuDomain) ? domain : qiNiuDomain;
    }
    /**
     * 七牛云上传图片大小   和【maxSize】二选一
     */
    private Long qiNiuMaxSize;
    /**
     * 七牛云允许文件类型白名单   和【fileWhiteSuffixNames】 二选一
     */
    @Builder.Default
    private Set<String> qiNiuWhite = new HashSet<>();

    public Set<String> getQiNiuWhite() {
        return CollectionUtils.isEmpty(qiNiuWhite) ? suffixNameWhites : qiNiuWhite;
    }

    @Builder.Default
    private Boolean qiNiuAutoDelete = false;

    /**
     * 七牛云配置结束   -----------------------------------------------------------------------------------
     */


    /**
     * 阿里云配置开始   -----------------------------------------------------------------------------------
     */

    /**
     *  阿里云密钥对  【必填】
     */
    private String aliAccessKey;
    private String aliSecretKey;
    /**
     * 地域节点  【必填】
     */
    private String aliEndpoint;
    /**
     * 空间名称  【必填】
     */
    private String aliBucketName;
    /**
     * 阿里云自定义域名  和【domain】二选一
     */
    private String aliDomain;
    public String getAliDomain() {
        return StringUtils.isEmpty(aliDomain) ? domain : aliDomain;
    }

    /**
     * 阿里云上传图片大小   和【maxSize】二选一
     */
    private Long aliMaxSize;
    /**
     * 阿里云允许文件类型白名单   和【fileWhiteSuffixNames】 二选一
     */
    @Builder.Default
    private Set<String> aliWhite = new HashSet<>();

    public Set<String> getAliWhite() {
        return CollectionUtils.isEmpty(aliWhite) ? suffixNameWhites : aliWhite;
    }

    /**
     * 阿里云配置结束   -----------------------------------------------------------------------------------
     */

    /**
     * 华为云配置开始   -----------------------------------------------------------------------------------
     */

    /**
     *  华为云密钥对  【必填】
     */
    private String huaweiAccessKey;
    private String huaweiSecretKey;
    /**
     * 地域节点  【必填】
     */
    private String huaweiEndpoint;
    /**
     * 空间名称  【必填】
     */
    private String huaweiBucketName;
    /**
     * 华为云区域名称
     */
    @Builder.Default
    private String huaweiArea = "cn-north-4";
    /**
     * 华为云自定义域名  和【domain】二选一
     */
    private String huaweiDomain;
    public String getHuaweiDomain() {
        return StringUtils.isEmpty(huaweiDomain) ? domain : huaweiDomain;
    }

    /**
     * 华为云上传图片大小   和【maxSize】二选一
     */
    private Long huaweiMaxSize;
    /**
     * 华为云允许文件类型白名单   和【fileWhiteSuffixNames】 二选一
     */
    @Builder.Default
    private Set<String> huaweiWhite = new HashSet<>();

    public Set<String> getHuaweiWhite() {
        return CollectionUtils.isEmpty(huaweiWhite) ? suffixNameWhites : huaweiWhite;
    }

    /**
     * 华为云配置结束   -----------------------------------------------------------------------------------
     */
}
