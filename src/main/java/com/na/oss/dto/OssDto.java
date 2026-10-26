package com.na.oss.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.na.oss.enums.OssFileOptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * OSS 文件操作参数与结果对象。
 * <p>
 * 上传、查询、下载、删除等操作的入参与返回值统一使用该对象。
 * 上传时通过 {@link #uploadFile} 或 {@link #inputStream} 提供文件内容；
 * 删除/查询时通过 {@link #storageFilePath} 传入文件全路径。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaOssDto {

    /**
     * 文件唯一标识（由业务方按需使用）
     */
    private String id;

    /**
     * 路径前缀，最终会拼接在存储路径最前面
     */
    private String pathPrefix;

    /**
     * 上传的文件，与 {@link #inputStream} 二选一
     */
    @JsonIgnore
    private MultipartFile uploadFile;

    /**
     * 文件输入流，与 {@link #uploadFile} 二选一。
     * <p>实现方读取完成后会关闭该流，调用方无需重复关闭。</p>
     */
    @JsonIgnore
    private InputStream inputStream;

    /**
     * 原始文件名（未指定时自动取上传文件的原始名称）
     */
    private String sourceFileName;

    /**
     * 新文件名，例如 {@code 1719471001213.JPG}；为空时由工具类自动生成
     */
    private String newFileName;

    /**
     * 存储路径。
     * <p>上传时表示目录前缀，例如 {@code img/2024/06/27/}；</p>
     * <p>删除/查询/下载时表示文件全路径，例如 {@code images/2024/04/13/1712995109396.jpg}。</p>
     */
    private String storageFilePath;

    /**
     * 存储空间名称（由具体实现回填）
     */
    private String bucket;

    /**
     * 操作状态（由具体实现回填）
     */
    private OssFileOptStatus status;

    /**
     * 访问域名（由具体实现回填）
     */
    private String domain;

    /**
     * 上传来源：七牛云 QINIU、阿里云 ALI、腾讯云 TENCENT、华为云 HUAWEI、本地 LOCAL
     */
    @Builder.Default
    private String fromType = FromType.QINIU;

    /**
     * 七牛云上传失败时的响应内容，用于排查原因。
     * <p>使用字符串而非 SDK 的 {@code Response} 类型，避免 DTO 强依赖七牛 SDK，
     * 使未引入七牛 SDK 的项目也能正常使用阿里云/华为云/本地存储。</p>
     */
    private String qiniuResponseBody;

    /**
     * 上传来源常量定义。
     * <p>使用静态内部类而非枚举，便于配置文件中直接以字符串使用。</p>
     */
    public static final class FromType {

        /** 七牛云 */
        public static final String QINIU = "QINIU";
        /** 阿里云 */
        public static final String ALI = "ALI";
        /** 腾讯云 */
        public static final String TENCENT = "TENCENT";
        /** 华为云 */
        public static final String HUAWEI = "HUAWEI";
        /** 本地磁盘 */
        public static final String LOCAL = "LOCAL";

        private FromType() {
        }
    }

    /**
     * 获取原始文件名。
     * <p>存在上传文件时优先返回上传文件的原始名称，否则返回手工设置的值。</p>
     *
     * @return 原始文件名，可能为 {@code null}
     */
    public String getSourceFileName() {
        return uploadFile == null ? sourceFileName : uploadFile.getOriginalFilename();
    }

    /**
     * 判断上传内容是否为空。
     *
     * @return {@code true} 表示既没有上传文件也没有输入流
     */
    @JsonIgnore
    public Boolean isFileNull() {
        return uploadFile == null && inputStream == null;
    }

    /**
     * 获取用于校验和生成文件名的新文件名。
     * <p>优先使用 {@link #newFileName}，为空时回退到原始文件名。</p>
     *
     * @return 文件名，可能为 {@code null}
     */
    @JsonIgnore
    public String getFileName() {
        return StringUtils.isBlank(newFileName) ? getSourceFileName() : newFileName;
    }

    /**
     * 判断存储路径是否为空。
     *
     * @return {@code true} 表示未设置 {@link #storageFilePath}
     */
    @JsonIgnore
    public Boolean isPathNull() {
        return storageFilePath == null;
    }

    /**
     * 使用文件全路径构造 DTO，常用于删除/查询场景。
     *
     * @param storageFilePath 文件全路径
     */
    public NaOssDto(String storageFilePath) {
        this.storageFilePath = storageFilePath;
    }
}
