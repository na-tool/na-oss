package com.na.oss.hardDisk.impl;

import com.na.oss.config.NaAutoOssConfig;
import com.na.oss.dto.NaOssDto;
import com.na.oss.enums.NaOssFileOptStatus;
import com.na.oss.hardDisk.INaOssHardDiskService;
import com.na.oss.utils.NaOssFileUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;

/**
 * 本地硬盘存储实现类
 */
@Service
public class NaOssHardDiskServiceImpl implements INaOssHardDiskService {

    @Autowired
    private NaAutoOssConfig autoOssConfig;

    /**
     * 上传文件到本地磁盘
     *
     * @param dto             上传文件 DTO
     * @param naAutoOssConfig 可选配置参数（为空则用默认配置）
     * @return 上传完成的 DTO
     */
    @Override
    public NaOssDto upload(NaOssDto dto, NaAutoOssConfig naAutoOssConfig) {
        dto.setFromType(NaOssDto.FromType.LOCAL);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;

        // 参数校验：本地路径、文件名、文件内容不能为空
        if (StringUtils.isEmpty(naAutoOssConfig.getLocalPath()) ||
                StringUtils.isEmpty(dto.getSourceFileName()) ||
               dto.isFileNull()) {
            dto.setStatus(NaOssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 文件类型校验
        if (!NaOssFileUtil.checkFileType(dto, naAutoOssConfig)) {
            dto.setStatus(NaOssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 文件大小校验
        if (!NaOssFileUtil.checkFileSize(dto, naAutoOssConfig)) {
            dto.setStatus(NaOssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 构建目标文件名和路径
        dto = NaOssFileUtil.conversionToDto(dto);

        // 拼接目标文件完整路径
        String fullPath = naAutoOssConfig.getLocalPath()
                + dto.getStorageFilePath()
                + dto.getNewFileName();

        File targetFile = new File(fullPath);

        // 创建目录（如果不存在）
        NaOssFileUtil.checkCreateFolder(targetFile);

        try {
            if (dto.getUploadFile() != null) {
                // Spring 上传组件提供直接写入方法
                dto.getUploadFile().transferTo(targetFile);
            } else if (dto.getInputStream() != null) {
                // 手动写入 InputStream 到文件
                try (InputStream in = dto.getInputStream();
                     OutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }

            // 上传成功后设置状态和相关元信息
            dto.setStatus(NaOssFileOptStatus.DONE);
            dto.setBucket(naAutoOssConfig.getLocalPath());
            dto.setDomain(naAutoOssConfig.getDomain());

        } catch (IOException e) {
            e.printStackTrace();
            dto.setStatus(NaOssFileOptStatus.ERROR);
        }

        return dto;
    }

    /**
     * 删除指定的本地文件
     *
     * @param dto             请求 DTO（需提供文件路径）
     * @param naAutoOssConfig 可选配置（为空时使用默认配置）
     * @return 删除结果 DTO
     */
    @Override
    public NaOssDto delete(NaOssDto dto, NaAutoOssConfig naAutoOssConfig) {
        dto.setFromType(NaOssDto.FromType.LOCAL);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;

        if (StringUtils.isEmpty(naAutoOssConfig.getLocalPath()) ||
                StringUtils.isEmpty(dto.getStorageFilePath())) {
            dto.setStatus(NaOssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        // 拼接完整文件路径
        String filePath = naAutoOssConfig.getLocalPath() + dto.getStorageFilePath();
        File file = new File(filePath);

        // 若文件存在，则删除
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                dto.setStatus(NaOssFileOptStatus.ERROR);
                return dto;
            }
        }

        dto.setStatus(NaOssFileOptStatus.REMOVE);
        dto.setBucket(naAutoOssConfig.getLocalPath());
        return dto;
    }
}
