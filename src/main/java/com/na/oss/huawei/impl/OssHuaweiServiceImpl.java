package com.na.oss.huawei.impl;

import com.na.oss.config.AutoOssConfig;
import com.na.oss.dto.OssDto;
import com.na.oss.enums.OssFileOptStatus;
import com.na.oss.huawei.IOssHuaweiService;
import com.na.oss.utils.OssFileUtil;
import com.obs.services.ObsClient;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.PutObjectRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 华为云 OBS 操作实现。
 * <p>
 * 由 {@code OssAutoConfiguration} 在配置了 {@code na.oss.huaweiAccessKey} 时自动装配。
 * 上传时会检查 Bucket 是否存在，不存在则按配置的区域自动创建；
 * 每次调用结束后都会关闭 {@link ObsClient}，避免连接泄漏。
 * </p>
 */
public class OssHuaweiServiceImpl implements IOssHuaweiService {

    private static final Logger log = LoggerFactory.getLogger(OssHuaweiServiceImpl.class);

    /**
     * 全局默认配置
     */
    private final AutoOssConfig autoOssConfig;

    /**
     * 构造器注入默认配置。
     *
     * @param autoOssConfig OSS 配置
     */
    public OssHuaweiServiceImpl(AutoOssConfig autoOssConfig) {
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
     * 创建华为云 OBS 客户端。
     *
     * @param config              OBS 配置
     * @param createBucketIfAbsent 是否在 Bucket 不存在时自动创建
     * @return OBS 客户端，使用后必须调用 {@link ObsClient#close()}
     */
    private ObsClient init(AutoOssConfig config, boolean createBucketIfAbsent) {
        ObsClient obsClient = new ObsClient(config.getHuaweiAccessKey(),
                config.getHuaweiSecretKey(),
                config.getHuaweiEndpoint());
        try {
            if (createBucketIfAbsent && !obsClient.headBucket(config.getHuaweiBucketName())) {
                ObsBucket obsBucket = new ObsBucket();
                obsBucket.setBucketName(config.getHuaweiBucketName());
                obsBucket.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
                obsBucket.setLocation(config.getHuaweiArea());
                obsClient.createBucket(obsBucket);
                log.info("华为云 Bucket 不存在，已自动创建: {}", config.getHuaweiBucketName());
            }
            return obsClient;
        } catch (RuntimeException e) {
            // 初始化失败时及时关闭客户端
            closeQuietly(obsClient);
            throw e;
        }
    }

    @Override
    public OssDto upload(OssDto dto, AutoOssConfig customConfig) throws IOException {
        dto.setFromType(OssDto.FromType.HUAWEI);
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
        String filePath = dto.getStorageFilePath() + dto.getNewFileName();

        ObsClient obsClient = init(config, true);
        try {
            // 构建对象元信息
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setCacheControl("no-cache");
            objectMetadata.setContentType(
                    OssFileUtil.getContentType(OssFileUtil.getExtension(dto.getFileName())));
            objectMetadata.setContentDisposition("inline; filename=\""
                    + OssFileUtil.encodeFileName(dto.getFileName()) + "\"");

            PutObjectRequest putObjectRequest = new PutObjectRequest();
            putObjectRequest.setBucketName(config.getHuaweiBucketName());
            putObjectRequest.setObjectKey(filePath);
            putObjectRequest.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            putObjectRequest.setMetadata(objectMetadata);

            if (dto.getUploadFile() != null) {
                // MultipartFile：Content-Length 取文件大小，输入流由本方法关闭
                objectMetadata.setContentLength(dto.getUploadFile().getSize());
                try (InputStream inputStream = dto.getUploadFile().getInputStream()) {
                    putObjectRequest.setInput(inputStream);
                    obsClient.putObject(putObjectRequest);
                }
            } else {
                // InputStream：先完整读入内存以确定 Content-Length，再执行上传；
                // 读取完成后由本方法关闭调用方传入的流
                try (InputStream source = dto.getInputStream()) {
                    byte[] bytes = OssFileUtil.readAllBytes(source);
                    objectMetadata.setContentLength((long) bytes.length);
                    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                        putObjectRequest.setInput(inputStream);
                        obsClient.putObject(putObjectRequest);
                    }
                }
            }

            dto.setStatus(OssFileOptStatus.DONE);
            dto.setDomain(config.getHuaweiDomain());
            dto.setBucket(config.getHuaweiBucketName());
            return dto;
        } catch (IOException | RuntimeException e) {
            dto.setStatus(OssFileOptStatus.ERROR);
            throw e;
        } finally {
            closeQuietly(obsClient);
        }
    }

    @Override
    public OssDto delete(OssDto dto, AutoOssConfig customConfig) {
        dto.setFromType(OssDto.FromType.HUAWEI);
        AutoOssConfig config = resolveConfig(customConfig);

        if (StringUtils.isBlank(dto.getStorageFilePath())) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 删除操作无需创建 Bucket
        ObsClient obsClient = init(config, false);
        try {
            // 根据 BucketName、对象全路径删除文件
            obsClient.deleteObject(config.getHuaweiBucketName(), dto.getStorageFilePath());
            dto.setStatus(OssFileOptStatus.REMOVE);
            dto.setDomain(config.getHuaweiDomain());
            dto.setBucket(config.getHuaweiBucketName());
            return dto;
        } catch (RuntimeException e) {
            dto.setStatus(OssFileOptStatus.ERROR);
            throw e;
        } finally {
            closeQuietly(obsClient);
        }
    }

    /**
     * 静默关闭 OBS 客户端。
     *
     * @param obsClient OBS 客户端
     */
    private void closeQuietly(ObsClient obsClient) {
        if (obsClient == null) {
            return;
        }
        try {
            obsClient.close();
        } catch (IOException e) {
            log.warn("关闭华为云 OBS 客户端失败", e);
        }
    }
}
