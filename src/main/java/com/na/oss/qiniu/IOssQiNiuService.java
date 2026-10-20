package com.na.oss.qiniu;

import com.na.oss.config.AutoOssConfig;
import com.na.oss.dto.OssDto;
import com.qiniu.common.QiniuException;

import java.io.IOException;

/**
 * 七牛云对象存储操作服务。
 */
public interface IOssQiNiuService {

    /**
     * 上传文件到七牛云。
     *
     * @param dto             上传参数，需包含文件内容
     * @param customConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 包含操作状态、Bucket、访问域名等信息的 DTO
     * @throws IOException 读取上传文件失败时抛出
     */
    OssDto upload(OssDto dto,
                    AutoOssConfig customConfig) throws IOException;

    /**
     * 删除七牛云文件。
     *
     * @param dto             删除参数，{@code storageFilePath} 需传文件全路径
     * @param customConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 包含操作状态的 DTO
     * @throws QiniuException 调用七牛云接口失败时抛出
     */
    OssDto delete(OssDto dto,
                    AutoOssConfig customConfig) throws QiniuException;
}
