package com.na.oss.hardDisk;

import com.na.oss.config.AutoOssConfig;
import com.na.oss.dto.OssDto;

import java.io.IOException;

/**
 * 本地磁盘存储服务。
 * <p>文件保存到 {@code na.oss.localPath} 指定的目录下。</p>
 */
public interface IOssHardDiskService {

    /**
     * 上传文件到本地磁盘。
     *
     * @param dto             上传参数，需包含文件内容
     * @param customConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 包含操作状态、存储目录等信息的 DTO
     * @throws IOException 写入文件失败时抛出
     */
    OssDto upload(OssDto dto,
                    AutoOssConfig customConfig) throws IOException;

    /**
     * 删除本地磁盘文件。
     *
     * @param dto             删除参数，{@code storageFilePath} 需传相对 {@code localPath} 的路径
     * @param customConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 包含操作状态的 DTO
     */
    OssDto delete(OssDto dto,
                    AutoOssConfig customConfig);
}
