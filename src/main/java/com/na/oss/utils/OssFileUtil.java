package com.na.oss.utils;

import com.na.oss.config.AutoOssConfig;
import com.na.oss.dto.OssDto;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * OSS 文件操作工具类。
 * <p>
 * 提供文件类型/大小校验、目标文件名与存储路径生成、目录创建、Content-Type 推断、
 * 本地路径越界防护等通用能力，供本地磁盘、阿里云、华为云、七牛云等实现复用。
 * </p>
 */
public final class OssFileUtil {

    /**
     * 默认允许上传的图片后缀。
     * <p>当全局白名单 {@code na.oss.suffixNameWhites} 未配置时作为兜底。</p>
     */
    public static final String[] FILE_TYPE = new String[]{".jpg", ".jpeg", ".gif", ".png"};

    /**
     * 分片上传允许的最小分片大小：100KB。
     * <p>阿里云 OSS 要求除最后一个分片外，每个分片不得小于 100KB。</p>
     */
    public static final long MIN_PART_SIZE = 100 * 1024L;

    /**
     * 无法识别文件类型时使用的默认 MIME 类型。
     */
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /**
     * 工具类禁止实例化。
     */
    private OssFileUtil() {
    }

    /**
     * 校验文件后缀是否在对应存储平台的白名单中。
     * <p>
     * 根据 {@link OssDto} 中的来源平台选择对应白名单，未匹配到平台时使用全局白名单。
     * 文件名优先取原始文件名，没有时回退到新文件名。
     * </p>
     *
     * @param dto           上传参数，需包含来源平台及文件名
     * @param autoOssConfig OSS 配置，不能为空
     * @return {@code true} 表示允许上传；{@code false} 表示文件名为空、白名单为空或后缀不匹配
     */
    public static Boolean checkFileType(OssDto dto, AutoOssConfig autoOssConfig) {
        if (dto == null || autoOssConfig == null) {
            return false;
        }

        // 根据文件来源选择对应的白名单
        Set<String> allowedSuffixes = new HashSet<>();
        String fromType = dto.getFromType();
        if (OssDto.FromType.LOCAL.equals(fromType)) {
            allowedSuffixes.addAll(autoOssConfig.getLocalWhite());
        } else if (OssDto.FromType.HUAWEI.equals(fromType)) {
            allowedSuffixes.addAll(autoOssConfig.getHuaweiWhite());
        } else if (OssDto.FromType.QINIU.equals(fromType)) {
            allowedSuffixes.addAll(autoOssConfig.getQiNiuWhite());
        } else if (OssDto.FromType.ALI.equals(fromType)) {
            allowedSuffixes.addAll(autoOssConfig.getAliWhite());
        } else {
            allowedSuffixes.addAll(autoOssConfig.getSuffixNameWhites());
        }

        // 优先使用原始文件名（真实内容类型），没有时回退到新文件名
        String fileName = StringUtils.defaultIfBlank(dto.getSourceFileName(), dto.getNewFileName());
        if (StringUtils.isBlank(fileName) || CollectionUtils.isEmpty(allowedSuffixes)) {
            return false;
        }

        // 忽略大小写，并兼容配置中未写“.”的场景（如 jpg 与 .jpg）
        return allowedSuffixes.stream()
                .filter(StringUtils::isNotBlank)
                .map(suffix -> suffix.startsWith(".") ? suffix : "." + suffix)
                .anyMatch(suffix -> StringUtils.endsWithIgnoreCase(fileName, suffix));
    }

    /**
     * 校验上传文件大小是否在限制范围内。
     * <p>
     * 平台级配置（如 {@code aliMaxSize}）为空或不大于 0 时回退到全局 {@code na.oss.maxSize}。
     * 对于不支持 mark 的输入流，无法在不消费流的前提下获取准确大小，
     * 此时跳过本地校验，由存储服务端限制。
     * </p>
     *
     * @param dto           上传参数，需包含上传文件或输入流
     * @param autoOssConfig OSS 配置，不能为空
     * @return {@code true} 表示大小合法；{@code false} 表示文件为空、缺失或超出限制
     */
    public static Boolean checkFileSize(OssDto dto, AutoOssConfig autoOssConfig) {
        if (dto == null || autoOssConfig == null) {
            return false;
        }

        // 平台级大小限制，未配置时回退到全局限制
        Long platformMaxSize;
        String fromType = dto.getFromType();
        if (OssDto.FromType.LOCAL.equals(fromType)) {
            platformMaxSize = autoOssConfig.getLocalMaxSize();
        } else if (OssDto.FromType.HUAWEI.equals(fromType)) {
            platformMaxSize = autoOssConfig.getHuaweiMaxSize();
        } else if (OssDto.FromType.QINIU.equals(fromType)) {
            platformMaxSize = autoOssConfig.getQiNiuMaxSize();
        } else if (OssDto.FromType.ALI.equals(fromType)) {
            platformMaxSize = autoOssConfig.getAliMaxSize();
        } else {
            platformMaxSize = autoOssConfig.getMaxSize();
        }

        long maxSize = (platformMaxSize == null || platformMaxSize <= 0)
                ? defaultMaxSize(autoOssConfig)
                : platformMaxSize;

        MultipartFile uploadFile = dto.getUploadFile();
        if (uploadFile != null) {
            long fileSize = uploadFile.getSize();
            return fileSize > 0 && fileSize <= maxSize;
        }

        InputStream inputStream = dto.getInputStream();
        if (inputStream == null) {
            return false;
        }

        // 无法安全获取大小的流不在此处拦截，交由存储服务端做最终校验
        if (!inputStream.markSupported()) {
            return true;
        }

        long fileSize = getInputStreamSize(inputStream);
        return fileSize < 0 || (fileSize > 0 && fileSize <= maxSize);
    }

    /**
     * 获取全局最大文件大小，配置非法时回退到 10MB。
     *
     * @param autoOssConfig OSS 配置
     * @return 全局最大文件大小（字节）
     */
    private static long defaultMaxSize(AutoOssConfig autoOssConfig) {
        Long maxSize = autoOssConfig.getMaxSize();
        return (maxSize == null || maxSize <= 0) ? 10 * 1024 * 1024L : maxSize;
    }

    /**
     * 自动构建 DTO 的目标文件名与存储路径。
     * <p>
     * 规则：
     * <ul>
     *     <li>新文件名为空时，按“时间戳 + 随机数 + 原始后缀”生成；</li>
     *     <li>存储路径为空时，默认使用当前日期（yyyy/MM/dd/）；</li>
     *     <li>配置了 {@code pathPrefix} 时，拼接在存储路径最前面。</li>
     * </ul>
     *
     * @param dto 原始 DTO
     * @return 构建完成后的 DTO（原对象）
     */
    public static OssDto conversionToDto(OssDto dto) {
        if (dto == null) {
            return null;
        }

        // 自动生成新文件名（格式：时间戳 + 随机数 + 原始后缀）
        if (StringUtils.isBlank(dto.getNewFileName())) {
            String suffix = StringUtils.substringAfterLast(StringUtils.defaultString(dto.getSourceFileName()), ".");
            String newFileName = System.currentTimeMillis() + RandomUtils.nextInt(100, 9999)
                    + (StringUtils.isEmpty(suffix) ? "" : "." + suffix);
            dto.setNewFileName(newFileName);
        }

        // 未指定存储目录时，默认按日期分目录
        String storagePath = trimSlashes(dto.getStorageFilePath());
        if (StringUtils.isEmpty(storagePath)) {
            storagePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        }

        // 拼接可选的路径前缀
        String prefix = trimSlashes(dto.getPathPrefix());
        if (StringUtils.isNotEmpty(prefix)) {
            storagePath = prefix + "/" + storagePath;
        }

        dto.setStorageFilePath(storagePath + "/");
        return dto;
    }

    /**
     * 移除字符串首尾的斜杠（/）。
     *
     * @param str 原始字符串
     * @return 去除首尾斜杠后的结果，入参为空时返回空字符串
     */
    private static String trimSlashes(String str) {
        if (StringUtils.isEmpty(str)) {
            return StringUtils.EMPTY;
        }
        return str.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    /**
     * 检查文件所在目录是否存在，不存在则递归创建。
     *
     * @param file 目标文件
     * @throws IllegalArgumentException 文件对象为空时抛出
     * @throws IllegalStateException    目录创建失败时抛出
     */
    public static void checkCreateFolder(File file) {
        if (file == null) {
            throw new IllegalArgumentException("文件对象不能为空");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            throw new IllegalStateException("创建目录失败: " + parent.getAbsolutePath());
        }
    }

    /**
     * 解析位于基准目录下的文件，防止路径穿越（如 {@code ../}）。
     * <p>
     * 通过规范化路径后比较前缀，确保最终文件始终位于 {@code basePath} 目录内。
     * </p>
     *
     * @param basePath     基准目录
     * @param relativePath 相对路径
     * @return 规范化后的文件对象；路径越界时返回 {@code null}
     * @throws IOException 路径规范化失败时抛出
     */
    public static File resolveUnderBase(String basePath, String relativePath) throws IOException {
        if (StringUtils.isBlank(basePath) || StringUtils.isBlank(relativePath)) {
            return null;
        }
        File base = new File(basePath).getCanonicalFile();
        File target = new File(base, relativePath).getCanonicalFile();
        // 使用 Path 按路径段比较，避免 "test" 与 "test2" 这类前缀误判，
        // 同时在 Windows 下按不区分大小写处理
        if (!target.toPath().startsWith(base.toPath())) {
            // 路径越出基准目录，拒绝操作
            return null;
        }
        return target;
    }

    /**
     * 获取输入流大小。
     * <p>
     * 优先使用 {@link InputStream#available()}（不消费流且无额外内存开销）；
     * 当 available 无法判断大小时，对支持 mark/reset 的流通过标记读取测量，并在读取完成后重置。
     * </p>
     *
     * @param inputStream 输入流
     * @return 流大小（字节）；读取失败时返回 -1
     */
    public static long getInputStreamSize(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream 不能为空");
        }

        try {
            // 对 ByteArrayInputStream、文件流等，available 即为剩余字节数
            int available = inputStream.available();
            if (available > 0 || !inputStream.markSupported()) {
                return available;
            }

            long size = 0;
            inputStream.mark(Integer.MAX_VALUE);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                size += bytesRead;
            }
            // 重置到读取前的位置，保证后续上传能读到完整内容
            inputStream.reset();
            return size;
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * 读取输入流的全部字节。
     * <p>用于需要先确定内容长度、再执行上传的场景（如阿里云/华为云对象存储）。</p>
     *
     * @param inputStream 输入流
     * @return 输入流中的全部字节
     * @throws IOException 读取失败时抛出
     */
    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream 不能为空");
        }
        return inputStream.readAllBytes();
    }

    /**
     * 根据文件名后缀推断 MIME 类型。
     * <p>
     * 上传时显式设置 Content-Type，可避免浏览器访问图片地址时触发下载。
     * </p>
     *
     * @param extension 文件后缀，支持带点（.jpg）或不带点（jpg）
     * @return 对应的 MIME 类型，无法识别时返回 {@link #DEFAULT_CONTENT_TYPE}
     */
    public static String getContentType(String extension) {
        if (StringUtils.isBlank(extension)) {
            return DEFAULT_CONTENT_TYPE;
        }

        String ext = extension.trim().toLowerCase(Locale.ROOT);
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }

        switch (ext) {
            // 图片
            case ".bmp":
                return "image/bmp";
            case ".gif":
                return "image/gif";
            case ".png":
                return "image/png";
            case ".jpeg":
            case ".jpg":
                return "image/jpeg";
            case ".webp":
                return "image/webp";
            case ".svg":
                return "image/svg+xml";

            // 文本
            case ".txt":
                return "text/plain";
            case ".html":
            case ".htm":
                return "text/html";
            case ".css":
                return "text/css";
            case ".js":
                return "application/javascript";
            case ".xml":
                return "text/xml";
            case ".json":
                return "application/json";

            // Word
            case ".doc":
                return "application/msword";
            case ".docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

            // Excel
            case ".xls":
                return "application/vnd.ms-excel";
            case ".xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            // PPT
            case ".ppt":
                return "application/vnd.ms-powerpoint";
            case ".pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";

            // PDF
            case ".pdf":
                return "application/pdf";

            // 音频
            case ".mp3":
                return "audio/mpeg";
            case ".wav":
                return "audio/wav";
            case ".aac":
                return "audio/aac";
            case ".ogg":
                return "audio/ogg";
            case ".m4a":
                return "audio/mp4";

            // 视频
            case ".mp4":
                return "video/mp4";
            case ".avi":
                return "video/x-msvideo";
            case ".mov":
                return "video/quicktime";
            case ".wmv":
                return "video/x-ms-wmv";
            case ".flv":
                return "video/x-flv";
            case ".mkv":
                return "video/x-matroska";

            // 压缩包
            case ".zip":
                return "application/zip";
            case ".rar":
                return "application/x-rar-compressed";
            case ".7z":
                return "application/x-7z-compressed";

            default:
                return DEFAULT_CONTENT_TYPE;
        }
    }

    /**
     * 对文件名进行 URL 编码，兼容中文及特殊字符。
     *
     * @param fileName 原始文件名
     * @return 编码后的文件名
     */
    public static String encodeFileName(String fileName) {
        return URLEncoder.encode(StringUtils.defaultString(fileName), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    /**
     * 从文件名中提取后缀（包含点，如 {@code .jpg}）。
     *
     * @param fileName 文件名
     * @return 文件后缀；文件名无后缀时返回空字符串
     */
    public static String getExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return StringUtils.EMPTY;
        }
        int index = fileName.lastIndexOf('.');
        return index < 0 ? StringUtils.EMPTY : fileName.substring(index);
    }

    /**
     * 根据绝对路径获取文件输入流。
     *
     * @param absPath 文件绝对路径
     * @return 文件输入流
     * @throws IOException 文件不存在或为空时抛出
     */
    public static InputStream getInputStreamByAbsPath(String absPath) throws IOException {
        File file = new File(absPath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在: " + absPath);
        }
        if (file.length() == 0) {
            throw new IOException("文件内容为空: " + absPath);
        }
        return new FileInputStream(file);
    }

    /**
     * 将图片字节数组转换为 Base64 Data URL。
     *
     * @param imageBytes 图片字节数组
     * @param mimeType   MIME 类型，为空时使用 {@link #DEFAULT_CONTENT_TYPE}
     * @return 形如 {@code data:image/png;base64,xxxx} 的字符串
     */
    public static String imageBytesToBase64DataUrl(byte[] imageBytes, String mimeType) {
        if (imageBytes == null) {
            throw new IllegalArgumentException("图片字节数组不能为空");
        }
        String type = StringUtils.isBlank(mimeType) ? DEFAULT_CONTENT_TYPE : mimeType;
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:" + type + ";base64," + base64;
    }
}
