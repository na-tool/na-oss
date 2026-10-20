package com.na.oss.ali;

import com.aliyun.oss.model.OSSObjectSummary;
import com.na.oss.config.AutoOssConfig;
import com.na.oss.dto.OssDto;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 阿里云 OSS 操作服务。
 * <p>提供文件上传（普通/分片）、查询、下载、删除等能力。</p>
 */
public interface IOssAliService {

    /**
     * 上传文件到阿里云 OSS。
     *
     * @param dto           上传参数，需包含文件内容
     * @param autoOssConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 包含操作状态、Bucket、访问域名等信息的 DTO
     * @throws IOException 读取上传文件失败时抛出
     */
    OssDto upload(OssDto dto,
                    AutoOssConfig autoOssConfig) throws IOException;

    /**
     * 分片上传文件到阿里云 OSS。
     *
     * @param dto           上传参数，需包含文件内容
     * @param autoOssConfig 自定义配置，为 {@code null} 时使用默认配置
     * @param partSize      分片大小（字节），为空或小于 100KB 时使用默认值 5MB
     * @return 包含操作状态、Bucket、访问域名等信息的 DTO
     * @throws IOException 读取上传文件失败时抛出
     */
    OssDto uploadSlice(OssDto dto,
                         AutoOssConfig autoOssConfig,
                         Long partSize) throws IOException;

    /**
     * 根据文件全路径查询文件。
     *
     * @param dto           查询参数，{@code storageFilePath} 需传全路径，
     *                      例如 {@code images/2024/02/02/1706867083325.png}
     * @param autoOssConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 包含文件名、路径、Bucket、访问域名等信息的 DTO
     */
    OssDto queryByFileName(OssDto dto,
                             AutoOssConfig autoOssConfig);

    /**
     * 查询 Bucket 下的文件列表。
     *
     * @param maxKeys       最大返回条数，为空或非法时默认 1000（OSS 上限）
     * @param autoOssConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 文件摘要列表
     */
    List<OSSObjectSummary> queryAll(Integer maxKeys,
                                    AutoOssConfig autoOssConfig);

    /**
     * 根据文件全路径下载文件，并以附件形式写入响应流。
     *
     * @param dto           下载参数，{@code storageFilePath} 需传全路径
     * @param response      HTTP 响应对象
     * @param autoOssConfig 自定义配置，为 {@code null} 时使用默认配置
     * @throws IOException 写入响应流失败时抛出
     */
    void downloadByFileName(OssDto dto,
                            HttpServletResponse response,
                            AutoOssConfig autoOssConfig) throws IOException;

    /**
     * 根据文件全路径删除文件。
     *
     * @param dto           删除参数，{@code storageFilePath} 需传全路径
     * @param autoOssConfig 自定义配置，为 {@code null} 时使用默认配置
     * @return 包含操作状态的 DTO
     */
    OssDto deleteByFileName(OssDto dto,
                              AutoOssConfig autoOssConfig);
}
