package com.na.oss.utils;

import com.na.oss.config.NaAutoOssConfig;
import com.na.oss.dto.NaOssDto;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * OSS 文件上传辅助工具类
 */
public class NaOssFileUtil {

    /**
     * 默认允许上传的图片格式（未启用）
     */
    public static final String[] FILE_TYPE = new String[]{".jpg", ".jpeg", ".gif", ".png"};

    /**
     * 检查文件类型是否在白名单中
     *
     * @param dto           包含文件来源、文件名等信息
     * @param autoOssConfig 系统配置，含各来源的白名单后缀
     * @return true 表示允许，false 表示不允许
     */
    public static Boolean checkFileType(NaOssDto dto, NaAutoOssConfig autoOssConfig) {
        if (dto == null || autoOssConfig == null) {
            return false;
        }

        Set<String> allowedSuffixes = new HashSet<>();
        String fromType = dto.getFromType();

        switch (fromType) {
            case NaOssDto.FromType.LOCAL:
                allowedSuffixes.addAll(autoOssConfig.getLocalWhite());
                break;
            case NaOssDto.FromType.HUAWEI:
                allowedSuffixes.addAll(autoOssConfig.getHuaweiWhite());
                break;
            case NaOssDto.FromType.QINIU:
                allowedSuffixes.addAll(autoOssConfig.getQiNiuWhite());
                break;
            case NaOssDto.FromType.ALI:
                allowedSuffixes.addAll(autoOssConfig.getAliWhite());
                break;
            default:
                allowedSuffixes.addAll(autoOssConfig.getSuffixNameWhites());
                break;
        }

        return allowedSuffixes.stream()
                .anyMatch(suffix -> StringUtils.endsWithIgnoreCase(dto.getSourceFileName(), suffix));
    }

    /**
     * 校验上传文件大小是否在限制范围内
     *
     * @param dto           上传参数
     * @param autoOssConfig 上传配置
     * @return true 表示文件大小合法；false 表示非法
     */
    public static Boolean checkFileSize(NaOssDto dto, NaAutoOssConfig autoOssConfig) {
        if (dto == null || StringUtils.isEmpty(dto.getFromType()) || autoOssConfig == null) {
            return false;
        }

        long maxSize;
        String fromType = dto.getFromType();

        switch (fromType) {
            case NaOssDto.FromType.LOCAL:
                maxSize = autoOssConfig.getLocalMaxSize();
                break;
            case NaOssDto.FromType.HUAWEI:
                maxSize = autoOssConfig.getHuaweiMaxSize();
                break;
            case NaOssDto.FromType.QINIU:
                maxSize = autoOssConfig.getQiNiuMaxSize();
                break;
            case NaOssDto.FromType.ALI:
                maxSize = autoOssConfig.getAliMaxSize();
                break;
            default:
                maxSize = autoOssConfig.getMaxSize();
                break;
        }

        try {
            MultipartFile uploadFile = dto.getUploadFile();
            long fileSize = (uploadFile != null)
                    ? uploadFile.getSize()
                    : (dto.getInputStream() != null ? dto.getInputStream().available() : 0L);

            return fileSize > 0 && fileSize <= maxSize;
        } catch (IOException e) {
            System.err.println("文件大小检测异常：" + e.getMessage());
            return false;
        }
    }

    /**
     * 自动构建 DTO 的目标文件名与存储路径
     *
     * @param dto 原始 DTO
     * @return 构建完成后的 DTO
     */
    public static NaOssDto conversionToDto(NaOssDto dto) {
        if (dto == null) {
            return null;
        }

        // 自动生成新文件名（格式：时间戳+随机数+原始后缀）
        if (StringUtils.isEmpty(dto.getNewFileName())) {
            String suffix = StringUtils.substringAfterLast(dto.getSourceFileName(), ".");
            String newFileName = System.currentTimeMillis() +
                    RandomUtils.nextInt(100, 9999) + "." + suffix;
            dto.setNewFileName(newFileName);
        }

        String storagePath = dto.getStorageFilePath();
        String prefix = trimSlashes(dto.getPathPrefix());

        if (StringUtils.isNotEmpty(storagePath)) {
            storagePath = trimSlashes(storagePath) + "/";
        } else {
            // 默认使用当前日期作为路径
            storagePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/")) + "/";
        }

        // 加上路径前缀（如果有）
        if (StringUtils.isNotEmpty(prefix)) {
            storagePath = prefix + "/" + storagePath;
        }

        dto.setStorageFilePath(storagePath);
        return dto;
    }

    /**
     * 移除字符串前后的斜杠（/）
     *
     * @param str 原始字符串
     * @return 去除首尾斜杠后的结果
     */
    private static String trimSlashes(String str) {
        if (StringUtils.isEmpty(str)) {
            return StringUtils.EMPTY;
        }
        return str.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    /**
     * 检查文件的上级目录是否存在,若不存在则创建,若存在则跳过
     *
     * @param file 文件
     */
    public static void checkCreateFolder(File file) {
        if (file == null) {
            throw new NullPointerException();
        }
        if (file.exists()) {
            return;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    public static long getInputStreamSize(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        long size = 0;
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;

            // 标记流的位置以便于重置
            if (inputStream.markSupported()) {
                inputStream.mark(Integer.MAX_VALUE);
            }

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                size += bytesRead;
            }

            // 如果流支持mark/reset, 重置到标记位置
            if (inputStream.markSupported()) {
                inputStream.reset();
            }

        } catch (IOException e) {
            System.err.println("Error reading InputStream: " + e.getMessage());
            e.printStackTrace();
        }

        return size;
    }

    /**
     * 解决问题，直接访问上传的图片地址，会让下载而不是直接访问
     * 设置设置 HTTP 头 里边的 Content-Type
     * txt 格式经过测试，不需要转换 上传之后就是 text/plain。其他未测试
     * 已知  如果 Content-Type = .jpeg 访问地址会直接下载，本方法也是解决此问题
     * @param FilenameExtension
     * @return
     */
    public static String getContentType(String FilenameExtension) {
        if (FilenameExtension.equalsIgnoreCase(".bmp")) {
            return "image/bmp";
        }
        if (FilenameExtension.equalsIgnoreCase(".gif")) {
            return "image/gif";
        }
        if (FilenameExtension.equalsIgnoreCase(".jpeg") ||
                FilenameExtension.equalsIgnoreCase(".jpg") ||
                FilenameExtension.equalsIgnoreCase(".png")) {
            return "image/jpg";
        }
        if (FilenameExtension.equalsIgnoreCase(".html")) {
            return "text/html";
        }

        if (FilenameExtension.equalsIgnoreCase(".txt")) {
            return "text/plain";
        }
        if (FilenameExtension.equalsIgnoreCase(".vsd")) {
            return "application/vnd.visio";
        }
        if (FilenameExtension.equalsIgnoreCase(".pptx") ||
                FilenameExtension.equalsIgnoreCase(".ppt")) {
            return "application/vnd.ms-powerpoint";
        }
        if (FilenameExtension.equalsIgnoreCase(".docx") ||
                FilenameExtension.equalsIgnoreCase(".doc")) {
            return "application/msword";
        }
        if (FilenameExtension.equalsIgnoreCase(".xml")) {
            return "text/xml";
        }
        return "image/jpg";
    }

    public static InputStream getInputStreamByAbsPath(String pdfOutputPath) throws IOException {
        // 检查文件是否存在
        File pdfFile = new File(pdfOutputPath);
        if (!pdfFile.exists()) {
            throw new FileNotFoundException("The file at " + pdfOutputPath + " does not exist.");
        }
        InputStream inputStream = new FileInputStream(pdfFile);
        if (inputStream.available() == 0) {
            throw new IOException("The file is empty.");
        }
        return inputStream;
    }
}
