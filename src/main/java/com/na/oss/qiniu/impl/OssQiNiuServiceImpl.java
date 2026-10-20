package com.na.oss.qiniu.impl;

import com.na.oss.config.AutoOssConfig;
import com.na.oss.dto.OssDto;
import com.na.oss.enums.OssFileOptStatus;
import com.na.oss.qiniu.IOssQiNiuService;
import com.na.oss.utils.OssFileUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * 七牛云对象存储实现。
 * <p>
 * 由 {@code OssAutoConfiguration} 在配置了 {@code na.oss.qiNiuAccessKey} 时自动装配。
 * 上传使用七牛云表单上传凭证（uploadToken），文件以覆盖方式写入。
 * </p>
 */
public class OssQiNiuServiceImpl implements IOssQiNiuService {

    private static final Logger log = LoggerFactory.getLogger(OssQiNiuServiceImpl.class);

    /**
     * 全局默认配置
     */
    private final AutoOssConfig autoOssConfig;

    /**
     * 构造器注入默认配置。
     *
     * @param autoOssConfig OSS 配置
     */
    public OssQiNiuServiceImpl(AutoOssConfig autoOssConfig) {
        this.autoOssConfig = autoOssConfig;
    }

    /**
     * 解析实际使用的配置：优先使用调用方传入的配置，否则使用全局默认配置。
     *
     * @param config 调用方配置，可为 {@code null}
     * @return 非空的配置对象
     */
    private AutoOssConfig resolveConfig(AutoOssConfig config) {
        return config != null ? config : autoOssConfig;
    }

    /**
     * 创建空间管理器（用于删除文件）。
     *
     * @param config 七牛云配置
     * @return 空间管理器
     */
    private BucketManager initBucketManager(AutoOssConfig config) {
        return new BucketManager(initAuth(config), new Configuration());
    }

    /**
     * 创建七牛云鉴权对象。
     *
     * @param config 七牛云配置
     * @return 鉴权对象
     */
    private Auth initAuth(AutoOssConfig config) {
        return Auth.create(config.getQiNiuAccessKey(), config.getQiNiuSecretKey());
    }

    /**
     * 创建上传管理器。
     *
     * @return 上传管理器
     */
    private UploadManager initUploadManager() {
        return new UploadManager(new Configuration());
    }

    /**
     * 获取上传凭证。
     *
     * @param config 七牛云配置
     * @return 上传凭证
     */
    private String getToken(AutoOssConfig config) {
        return initAuth(config).uploadToken(config.getQiNiuBucketName());
    }

    /**
     * 读取七牛云失败响应内容，便于调用方排查原因。
     *
     * @param response 七牛云响应
     * @return 响应内容；读取失败时返回 {@code null}
     */
    private String readResponseBody(Response response) {
        try {
            return response.bodyString();
        } catch (QiniuException e) {
            log.warn("读取七牛云响应内容失败", e);
            return null;
        }
    }

    @Override
    public OssDto upload(OssDto dto, AutoOssConfig customConfig) throws IOException {
        dto.setFromType(OssDto.FromType.QINIU);
        AutoOssConfig config = resolveConfig(customConfig);

        if (dto.isFileNull()) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 文件类型校验
        if (!OssFileUtil.checkFileType(dto, config)) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 文件大小校验
        if (!OssFileUtil.checkFileSize(dto, config)) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 构建目标文件名和路径
        dto = OssFileUtil.conversionToDto(dto);
        String objectName = dto.getStorageFilePath() + dto.getNewFileName();

        // 可选：上传前先删除同路径旧文件（七牛云默认同名覆盖，按需开启）
        if (Boolean.TRUE.equals(config.getQiNiuAutoDelete())) {
            try {
                // 注意删除时传递的为全路径，例如 images/2024/04/13/1712995109396.jpg
                delete(new OssDto(objectName), config);
            } catch (Exception e) {
                // 旧文件可能本就不存在，删除失败不阻塞本次上传
                log.warn("七牛云旧文件删除失败, object={}", objectName, e);
            }
        }

        String token = getToken(config);
        UploadManager uploadManager = initUploadManager();

        Response response;
        if (dto.getUploadFile() != null) {
            // MultipartFile 的输入流由本方法负责关闭
            try (InputStream inputStream = dto.getUploadFile().getInputStream()) {
                response = uploadManager.put(inputStream, objectName, token, null, null);
            }
        } else {
            // 调用方传入的 InputStream 在读取完成后由本方法关闭
            try (InputStream inputStream = dto.getInputStream()) {
                response = uploadManager.put(inputStream, objectName, token, null, null);
            }
        }

        if (!response.isOK()) {
            dto.setStatus(OssFileOptStatus.ERROR);
            // 保留响应内容，便于调用方排查失败原因
            dto.setQiniuResponseBody(readResponseBody(response));
            return dto;
        }

        dto.setStatus(OssFileOptStatus.DONE);
        dto.setDomain(config.getQiNiuDomain());
        dto.setBucket(config.getQiNiuBucketName());
        return dto;
    }

    @Override
    public OssDto delete(OssDto dto, AutoOssConfig customConfig) throws QiniuException {
        dto.setFromType(OssDto.FromType.QINIU);
        AutoOssConfig config = resolveConfig(customConfig);

        if (StringUtils.isBlank(dto.getStorageFilePath())) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        BucketManager bucketManager = initBucketManager(config);
        // 使用本次解析出的配置删除，避免自定义配置场景下删错 Bucket
        bucketManager.delete(config.getQiNiuBucketName(), dto.getStorageFilePath());

        dto.setStatus(OssFileOptStatus.REMOVE);
        dto.setDomain(config.getQiNiuDomain());
        dto.setBucket(config.getQiNiuBucketName());
        return dto;
    }
}
