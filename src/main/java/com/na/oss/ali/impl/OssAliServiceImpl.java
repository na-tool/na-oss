package com.na.oss.ali.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.na.oss.ali.IOssAliService;
import com.na.oss.config.AutoOssConfig;
import com.na.oss.dto.OssDto;
import com.na.oss.enums.OssFileOptStatus;
import com.na.oss.utils.OssFileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 阿里云 OSS 操作实现。
 * <p>
 * 由 {@code OssAutoConfiguration} 在配置了 {@code na.oss.aliAccessKey} 时自动装配。
 * 每次调用都会创建独立的 {@link OSSClient}，并在使用完毕后关闭，避免连接泄漏。
 * </p>
 */
public class OssAliServiceImpl implements IOssAliService {

    private static final Logger log = LoggerFactory.getLogger(OssAliServiceImpl.class);

    /** 默认分片大小：5MB */
    private static final long DEFAULT_PART_SIZE = 5 * 1024 * 1024L;

    /** OSS 单次列举文件的最大条数 */
    private static final int MAX_LIST_KEYS = 1000;

    /**
     * 全局默认配置
     */
    private final AutoOssConfig autoOssConfig;

    /**
     * 构造器注入默认配置。
     *
     * @param autoOssConfig OSS 配置
     */
    public OssAliServiceImpl(AutoOssConfig autoOssConfig) {
        this.autoOssConfig = autoOssConfig;
    }

    /**
     * 创建阿里云 OSS 客户端。
     *
     * @param config OSS 配置
     * @return OSS 客户端，使用后必须调用 {@link OSS#shutdown()}
     */
    private OSS init(AutoOssConfig config) {
        return new OSSClient(config.getAliEndpoint(),
                config.getAliAccessKey(),
                config.getAliSecretKey());
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

    @Override
    public OssDto upload(OssDto dto, AutoOssConfig customConfig) throws IOException {
        dto.setFromType(OssDto.FromType.ALI);
        AutoOssConfig config = resolveConfig(customConfig);

        // 文件内容校验
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
        String bucketName = config.getAliBucketName();

        OSS ossClient = init(config);
        try {
            if (dto.getUploadFile() != null) {
                // MultipartFile：直接使用原始流，Content-Length 取文件大小
                try (InputStream inputStream = dto.getUploadFile().getInputStream()) {
                    ObjectMetadata metadata = buildMetadata(dto, dto.getUploadFile().getSize());
                    ossClient.putObject(bucketName, filePath, inputStream, metadata);
                }
            } else {
                // InputStream：先完整读入内存以确定 Content-Length，再执行上传；
                // 读取完成后由本方法关闭调用方传入的流
                try (InputStream source = dto.getInputStream()) {
                    byte[] bytes = OssFileUtil.readAllBytes(source);
                    try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                        ObjectMetadata metadata = buildMetadata(dto, (long) bytes.length);
                        ossClient.putObject(bucketName, filePath, inputStream, metadata);
                    }
                }
            }

            dto.setStatus(OssFileOptStatus.DONE);
            dto.setDomain(config.getAliDomain());
            dto.setBucket(bucketName);
            return dto;
        } catch (IOException | RuntimeException e) {
            dto.setStatus(OssFileOptStatus.ERROR);
            throw e;
        } finally {
            // 关闭客户端，释放连接资源
            ossClient.shutdown();
        }
    }

    @Override
    public OssDto uploadSlice(OssDto dto, AutoOssConfig customConfig, Long partSize) throws IOException {
        dto.setFromType(OssDto.FromType.ALI);
        AutoOssConfig config = resolveConfig(customConfig);

        // 文件为空
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

        // 转换文件名称
        dto = OssFileUtil.conversionToDto(dto);
        String objectName = dto.getStorageFilePath() + dto.getNewFileName();
        String bucketName = config.getAliBucketName();

        // 分片大小：未指定或小于 OSS 最小分片（100KB）时使用默认值 5MB
        long sliceSize = (partSize == null || partSize < OssFileUtil.MIN_PART_SIZE)
                ? DEFAULT_PART_SIZE
                : partSize;

        OSS ossClient = init(config);
        File tempFile = null;
        String uploadId = null;
        try {
            // 将 MultipartFile / InputStream 统一写入临时文件，便于按分片随机读取
            tempFile = File.createTempFile("na-oss-", ".tmp");
            if (dto.getUploadFile() != null) {
                try (InputStream inputStream = dto.getUploadFile().getInputStream()) {
                    Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                try (InputStream inputStream = dto.getInputStream()) {
                    Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            long fileSize = tempFile.length();
            if (fileSize <= 0) {
                dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
                return dto;
            }

            // 初始化分片上传，并写入 Content-Type 等元信息
            InitiateMultipartUploadRequest initiateRequest =
                    new InitiateMultipartUploadRequest(bucketName, objectName, buildMetadata(dto, null));
            uploadId = ossClient.initiateMultipartUpload(initiateRequest).getUploadId();

            // 保存所有已上传分片的 ETag
            List<PartETag> partETags = new ArrayList<>();

            // 计算分片数量（向上取整）
            int partCount = (int) ((fileSize + sliceSize - 1) / sliceSize);

            for (int i = 0; i < partCount; i++) {
                long startPos = i * sliceSize;
                long currentPartSize = Math.min(sliceSize, fileSize - startPos);

                // 每个分片重新打开文件流，并定位到分片起始位置
                try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                    skipFully(inputStream, startPos);

                    UploadPartRequest uploadPartRequest = new UploadPartRequest();
                    uploadPartRequest.setBucketName(bucketName);
                    uploadPartRequest.setKey(objectName);
                    uploadPartRequest.setUploadId(uploadId);
                    uploadPartRequest.setInputStream(inputStream);
                    uploadPartRequest.setPartSize(currentPartSize);
                    // 分片号从 1 开始
                    uploadPartRequest.setPartNumber(i + 1);

                    partETags.add(ossClient.uploadPart(uploadPartRequest).getPartETag());
                }
            }

            // OSS 要求按分片号排序后再合并
            Collections.sort(partETags, Comparator.comparingInt(PartETag::getPartNumber));

            // 合并所有分片
            CompleteMultipartUploadRequest completeRequest =
                    new CompleteMultipartUploadRequest(bucketName, objectName, uploadId, partETags);
            ossClient.completeMultipartUpload(completeRequest);
            // 合并成功后无需再执行取消操作
            uploadId = null;

            dto.setStatus(OssFileOptStatus.DONE);
            dto.setDomain(config.getAliDomain());
            dto.setBucket(bucketName);
            return dto;
        } catch (Exception e) {
            // 上传失败时取消分片任务，避免产生未完成的分片占用存储
            if (uploadId != null) {
                abortMultipartUpload(ossClient, bucketName, objectName, uploadId);
            }
            dto.setStatus(OssFileOptStatus.ERROR);
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new RuntimeException("OSS 分片上传失败", e);
        } finally {
            // 删除临时文件
            deleteTempFile(tempFile);
            // 关闭 OSS 客户端
            ossClient.shutdown();
        }
    }

    @Override
    public OssDto queryByFileName(OssDto dto, AutoOssConfig customConfig) {
        dto.setFromType(OssDto.FromType.ALI);
        AutoOssConfig config = resolveConfig(customConfig);

        if (StringUtils.isBlank(dto.getStorageFilePath())) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        OSS ossClient = init(config);
        // OSSObject 持有网络连接，必须关闭
        try (OSSObject object = ossClient.getObject(config.getAliBucketName(), dto.getStorageFilePath())) {
            String key = object.getKey();
            int separatorIndex = key.lastIndexOf('/');
            // 路径部分（包含末尾的 /）与文件名部分
            dto.setStorageFilePath(separatorIndex >= 0 ? key.substring(0, separatorIndex + 1) : "");
            dto.setNewFileName(separatorIndex >= 0 ? key.substring(separatorIndex + 1) : key);
            dto.setStatus(OssFileOptStatus.QUERY_SUCCESS);
            dto.setBucket(object.getBucketName());
            dto.setDomain(config.getAliDomain());
            return dto;
        } catch (IOException e) {
            // OSSObject.close() 可能抛出 IOException
            dto.setStatus(OssFileOptStatus.ERROR);
            throw new RuntimeException("关闭 OSS 对象失败", e);
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public List<OSSObjectSummary> queryAll(Integer maxKeys, AutoOssConfig customConfig) {
        AutoOssConfig config = resolveConfig(customConfig);
        OSS ossClient = init(config);
        try {
            // OSS 单次最多返回 1000 条
            int limit = (maxKeys == null || maxKeys <= 0) ? MAX_LIST_KEYS : Math.min(maxKeys, MAX_LIST_KEYS);
            ObjectListing objectListing = ossClient.listObjects(
                    new ListObjectsRequest(config.getAliBucketName()).withMaxKeys(limit));
            return objectListing.getObjectSummaries();
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public void downloadByFileName(OssDto dto, HttpServletResponse response,
                                   AutoOssConfig customConfig) throws IOException {
        dto.setFromType(OssDto.FromType.ALI);
        AutoOssConfig config = resolveConfig(customConfig);

        if (StringUtils.isBlank(dto.getStorageFilePath())) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return;
        }

        OSS ossClient = init(config);
        try {
            String storageFilePath = dto.getStorageFilePath();
            String fileName = storageFilePath.substring(storageFilePath.lastIndexOf('/') + 1);

            // 通知浏览器以附件形式下载，文件名做 URL 编码以兼容中文；
            // filename* 遵循 RFC 5987，兼容更多浏览器
            String encodedFileName = OssFileUtil.encodeFileName(fileName);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            // 根据文件后缀推断 MIME 类型
            response.setContentType(OssFileUtil.getContentType(OssFileUtil.getExtension(fileName)));

            // OSSObject 与输出流均通过 try-with-resources 自动关闭
            try (OSSObject ossObject = ossClient.getObject(config.getAliBucketName(), storageFilePath);
                 BufferedInputStream in = new BufferedInputStream(ossObject.getObjectContent());
                 BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream())) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
                out.flush();
            }

            dto.setStatus(OssFileOptStatus.QUERY_SUCCESS);
        } catch (IOException | RuntimeException e) {
            dto.setStatus(OssFileOptStatus.ERROR);
            throw e;
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public OssDto deleteByFileName(OssDto dto, AutoOssConfig customConfig) {
        dto.setFromType(OssDto.FromType.ALI);
        AutoOssConfig config = resolveConfig(customConfig);

        if (StringUtils.isBlank(dto.getStorageFilePath())) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        OSS ossClient = init(config);
        try {
            // 根据 BucketName、objectName 删除文件
            ossClient.deleteObject(config.getAliBucketName(), dto.getStorageFilePath());
            dto.setDomain(config.getAliDomain());
            dto.setBucket(config.getAliBucketName());
            dto.setStatus(OssFileOptStatus.REMOVE);
            return dto;
        } catch (RuntimeException e) {
            dto.setStatus(OssFileOptStatus.ERROR);
            throw e;
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 构建对象元信息（Content-Length、缓存策略、Content-Type、下载文件名）。
     *
     * @param dto           上传参数
     * @param contentLength 内容长度；为 {@code null} 时不设置（分片上传场景）
     * @return 对象元信息
     */
    private ObjectMetadata buildMetadata(OssDto dto, Long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        if (contentLength != null && contentLength >= 0) {
            metadata.setContentLength(contentLength);
        }
        metadata.setCacheControl("no-cache");
        metadata.setHeader("Pragma", "no-cache");
        metadata.setContentType(OssFileUtil.getContentType(OssFileUtil.getExtension(dto.getFileName())));
        metadata.setContentDisposition("inline; filename=\""
                + OssFileUtil.encodeFileName(dto.getFileName()) + "\"");
        return metadata;
    }

    /**
     * 跳过指定字节数，确保定位到分片起始位置。
     *
     * @param inputStream 输入流
     * @param bytes       需要跳过的字节数
     * @throws IOException 跳过失败或提前到达文件末尾时抛出
     */
    private void skipFully(InputStream inputStream, long bytes) throws IOException {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
            } else if (inputStream.read() == -1) {
                throw new EOFException("跳过文件分片时到达文件末尾");
            } else {
                remaining--;
            }
        }
    }

    /**
     * 取消分片上传任务。
     *
     * @param ossClient  OSS 客户端
     * @param bucketName Bucket 名称
     * @param objectName 对象名称
     * @param uploadId   分片上传任务 ID
     */
    private void abortMultipartUpload(OSS ossClient, String bucketName, String objectName, String uploadId) {
        try {
            ossClient.abortMultipartUpload(
                    new AbortMultipartUploadRequest(bucketName, objectName, uploadId));
        } catch (Exception ex) {
            log.warn("取消 OSS 分片上传失败, object={}, uploadId={}", objectName, uploadId, ex);
        }
    }

    /**
     * 删除临时文件；删除失败时注册 JVM 退出删除，避免残留。
     *
     * @param tempFile 临时文件
     */
    private void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
            log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
            tempFile.deleteOnExit();
        }
    }
}
