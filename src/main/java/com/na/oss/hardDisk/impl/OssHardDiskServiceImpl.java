package com.na.oss.hardDisk.impl;

import com.na.oss.config.AutoOssConfig;
import com.na.oss.dto.OssDto;
import com.na.oss.enums.OssFileOptStatus;
import com.na.oss.hardDisk.IOssHardDiskService;
import com.na.oss.utils.OssFileUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 本地磁盘存储实现。
 * <p>
 * 所有文件读写都限制在 {@code na.oss.localPath} 目录内，
 * 通过规范化路径校验防止 {@code ../} 路径穿越。
 * </p>
 * <p>由 {@code OssAutoConfiguration} 自动装配，无需组件扫描。</p>
 */
public class OssHardDiskServiceImpl implements IOssHardDiskService {

    /**
     * 全局默认配置
     */
    private final AutoOssConfig autoOssConfig;

    /**
     * 构造器注入默认配置。
     *
     * @param autoOssConfig OSS 配置
     */
    public OssHardDiskServiceImpl(AutoOssConfig autoOssConfig) {
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
     * 上传文件到本地磁盘。
     *
     * @param dto             上传文件 DTO
     * @param customConfig 可选配置参数（为空则用默认配置）
     * @return 上传完成的 DTO
     * @throws IOException 写入文件失败时抛出
     */
    @Override
    public OssDto upload(OssDto dto, AutoOssConfig customConfig) throws IOException {
        dto.setFromType(OssDto.FromType.LOCAL);
        AutoOssConfig config = resolveConfig(customConfig);

        // 参数校验：本地路径、文件名、文件内容不能为空
        if (StringUtils.isEmpty(config.getLocalPath())
                || StringUtils.isBlank(dto.getFileName())
                || dto.isFileNull()) {
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

        // 规范化并校验目标路径，防止路径穿越到 localPath 之外
        String relativePath = dto.getStorageFilePath() + dto.getNewFileName();
        File targetFile = OssFileUtil.resolveUnderBase(config.getLocalPath(), relativePath);
        if (targetFile == null) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        try {
            // 创建目录（如果不存在）
            OssFileUtil.checkCreateFolder(targetFile);

            if (dto.getUploadFile() != null) {
                // Spring 上传组件提供直接写入方法
                dto.getUploadFile().transferTo(targetFile);
            } else {
                // 手动写入 InputStream 到文件
                try (InputStream in = dto.getInputStream();
                     OutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            dto.setStatus(OssFileOptStatus.ERROR);
            throw e;
        }

        // 上传成功后设置状态和相关元信息
        dto.setStatus(OssFileOptStatus.DONE);
        dto.setBucket(config.getLocalPath());
        dto.setDomain(config.getDomain());
        return dto;
    }

    /**
     * 删除指定的本地文件。
     * <p>文件不存在时视为删除成功（幂等）。</p>
     *
     * @param dto             请求 DTO（需提供文件相对路径）
     * @param customConfig 可选配置（为空时使用默认配置）
     * @return 删除结果 DTO
     */
    @Override
    public OssDto delete(OssDto dto, AutoOssConfig customConfig) {
        dto.setFromType(OssDto.FromType.LOCAL);
        AutoOssConfig config = resolveConfig(customConfig);

        if (StringUtils.isEmpty(config.getLocalPath())
                || StringUtils.isBlank(dto.getStorageFilePath())) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 规范化并校验目标路径，防止通过 ../ 删除 localPath 之外的文件
        File file;
        try {
            file = OssFileUtil.resolveUnderBase(config.getLocalPath(), dto.getStorageFilePath());
        } catch (IOException e) {
            dto.setStatus(OssFileOptStatus.ERROR);
            return dto;
        }
        if (file == null) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 拒绝删除目录，避免误删整个存储目录
        if (file.exists() && file.isDirectory()) {
            dto.setStatus(OssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 若文件存在，则删除；不存在时按幂等成功处理
        if (file.exists() && !file.delete()) {
            dto.setStatus(OssFileOptStatus.ERROR);
            return dto;
        }

        dto.setStatus(OssFileOptStatus.REMOVE);
        dto.setBucket(config.getLocalPath());
        dto.setDomain(config.getDomain());
        return dto;
    }
}
