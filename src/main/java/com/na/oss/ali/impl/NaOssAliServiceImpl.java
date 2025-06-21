package com.na.oss.ali.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.*;
import com.na.oss.ali.INaOssAliService;
import com.na.oss.config.NaAutoOssConfig;
import com.na.oss.dto.NaOssDto;
import com.na.oss.enums.NaOssFileOptStatus;
import com.na.oss.utils.NaOssFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class NaOssAliServiceImpl implements INaOssAliService {

    @Autowired
    private NaAutoOssConfig autoOssConfig;

    private OSS init(NaAutoOssConfig autoOssConfig) {
        return new OSSClient(autoOssConfig.getAliEndpoint(),
                autoOssConfig.getAliAccessKey(),
                autoOssConfig.getAliSecretKey());
    }

    @Override
    public NaOssDto upload(NaOssDto dto, NaAutoOssConfig naAutoOssConfig) {
        dto.setFromType(NaOssDto.FromType.ALI);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;

        if(dto.isFileNull()){
            dto.setStatus(NaOssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        OSS ossClient = init(naAutoOssConfig);

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

        String filePath = dto.getStorageFilePath() + dto.getNewFileName();

        /**
         * 上传到阿里云
         */
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(dto.getUploadFile() != null ?
                    new ByteArrayInputStream(dto.getUploadFile().getBytes()).available() : NaOssFileUtil.getInputStreamSize(dto.getInputStream()));
            objectMetadata.setCacheControl("no-cache");
            objectMetadata.setHeader("Pragma", "no-cache");
            objectMetadata.setContentType(NaOssFileUtil.getContentType(dto.getFileName().substring(dto.getFileName().lastIndexOf("."))));
            objectMetadata.setContentDisposition("inline; filename=\"" + dto.getFileName() + "\"");
            ossClient.putObject(naAutoOssConfig.getAliBucketName(), filePath, dto.getUploadFile() != null ? new
                    ByteArrayInputStream(dto.getUploadFile().getBytes()) : dto.getInputStream(),objectMetadata);
        } catch (Exception e) {
            e.printStackTrace();
            //上传失败
            dto.setStatus(NaOssFileOptStatus.ERROR);
            return dto;
        }
        dto.setStatus(NaOssFileOptStatus.DONE);
        dto.setDomain(naAutoOssConfig.getAliDomain());
        dto.setBucket(naAutoOssConfig.getAliBucketName());
        return dto;
    }

    @Override
    public NaOssDto queryByFileName(NaOssDto dto, NaAutoOssConfig naAutoOssConfig) {
        dto.setFromType(NaOssDto.FromType.ALI);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;
        if(dto.isPathNull()){
            dto.setStatus(NaOssFileOptStatus.DATA_CHECK_ERROR);
            return dto;
        }

        OSS ossClient = init(naAutoOssConfig);
        /**
         * 根据BucketName,objectName查找文件文件
         */
        OSSObject object = ossClient.getObject(naAutoOssConfig.getAliBucketName(), dto.getStorageFilePath());
        dto.setStatus(NaOssFileOptStatus.QUERY_SUCCESS);
        dto.setBucket(object.getBucketName());
        String key = object.getKey();
        /**
         * 获取路径部分
         */
        String path = key.substring(0, key.lastIndexOf("/") + 1);
        /**
         * 获取文件名部分
         */
        String fileName = key.substring(key.lastIndexOf("/") + 1);
        dto.setNewFileName(fileName);
        dto.setStorageFilePath(path);
        dto.setDomain(naAutoOssConfig.getAliDomain());
        return dto;
    }

    @Override
    public List<OSSObjectSummary> queryAll(Integer maxKeys, NaAutoOssConfig naAutoOssConfig) {
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;
        OSS ossClient = init(naAutoOssConfig);

        /**
         * 列举文件
         */
        ObjectListing objectListing = ossClient.listObjects(new ListObjectsRequest(naAutoOssConfig.getAliBucketName()).withMaxKeys(maxKeys));
        List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
        return sums;
    }

    @Override
    public void downloadByFileName(NaOssDto dto, HttpServletResponse response, NaAutoOssConfig naAutoOssConfig) throws IOException {
        dto.setFromType(NaOssDto.FromType.ALI);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;

        if(dto.isPathNull()){
            dto.setStatus(NaOssFileOptStatus.ERROR);
            return;
        }

        OSS ossClient = init(naAutoOssConfig);

        String fileName = dto.getStorageFilePath().substring(dto.getStorageFilePath().lastIndexOf("/") + 1);
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
        /**
         * 通知浏览器以附件形式下载
         */
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);

        /**
         * 获取文件的MIME类型
         */
        String mimeType = Files.probeContentType(Paths.get(fileName));
        if (mimeType == null) {
            /**
             * 如果无法确定MIME类型，默认设置为二进制流
             */
            mimeType = "application/octet-stream";
        }
        response.setContentType(mimeType);

        ServletOutputStream os = response.getOutputStream();
        /**
         * ossObject包含文件所在的存储空间名称、文件名称、文件元信息以及一个输入流
         */
        OSSObject ossObject = ossClient.getObject(naAutoOssConfig.getAliBucketName(), dto.getStorageFilePath());
        /**
         * 读取文件内容
         */
        BufferedInputStream in = new BufferedInputStream(ossObject.getObjectContent());
        BufferedOutputStream out = new BufferedOutputStream(os);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }

        /**
         * 关闭流
         */
        if (out != null) {
            out.flush();
            out.close();
        }
        if (in != null) {
            in.close();
        }
    }

    @Override
    public NaOssDto deleteByFileName(NaOssDto dto, NaAutoOssConfig naAutoOssConfig) {
        dto.setFromType(NaOssDto.FromType.ALI);
        naAutoOssConfig = (naAutoOssConfig != null) ? naAutoOssConfig : autoOssConfig;
        if(dto.isPathNull()){
            dto.setStatus(NaOssFileOptStatus.ERROR);
            return dto;
        }

        OSS ossClient= init(naAutoOssConfig);

        /**
         * 根据BucketName,objectName删除文件
         */
        ossClient.deleteObject(naAutoOssConfig.getAliBucketName(), dto.getStorageFilePath());
        dto.setDomain(naAutoOssConfig.getAliDomain());
        dto.setBucket(naAutoOssConfig.getAliBucketName());
        dto.setStatus(NaOssFileOptStatus.REMOVE);
        return dto;
    }
}
