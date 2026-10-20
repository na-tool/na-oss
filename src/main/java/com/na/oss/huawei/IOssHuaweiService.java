package com.na.oss.huawei;

import com.na.oss.config.AutoOssConfig;
import com.na.oss.dto.OssDto;

import java.io.IOException;

/**
 * 华为云 OBS 操作服务。
 * <p>开发文档：<a href="https://support.huaweicloud.com/sdk-java-devg-obs/obs_21_0406.html">
 * https://support.huaweicloud.com/sdk-java-devg-obs/obs_21_0406.html</a></p>
 */
public interface IOssHuaweiService {

    /**
     * 上传文件到华为云 OBS。
     *
     * @param dto             上传参数，需包含文件内容
     * @param customConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 包含操作状态、Bucket、访问域名等信息的 DTO
     * @throws IOException 读取上传文件失败时抛出
     */
    OssDto upload(OssDto dto,
                    AutoOssConfig customConfig) throws IOException;

    /**
     * 删除华为云 OBS 文件。
     *
     * @param dto             删除参数，{@code storageFilePath} 需传文件全路径
     * @param customConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 包含操作状态的 DTO
     */
    OssDto delete(OssDto dto,
                    AutoOssConfig customConfig);
}
