### oss 模块

```text
当前版本：3.0.0
JDK：17+
Spring Boot：3.x（Servlet API 使用 jakarta.servlet）
支持：本地磁盘、七牛云、阿里云 OSS、华为云 OBS
```

#### 1 配置

##### 1.1 添加依赖

```xml
<dependency>
    <groupId>io.github.na-tool</groupId>
    <artifactId>na-oss</artifactId>
    <version>3.0.0</version>
</dependency>
```

`na-oss` 中第三方 SDK 均以 `provided` 方式声明，使用方需自行引入实际使用的存储 SDK：

```xml
<dependency>
    <groupId>com.qiniu</groupId>
    <artifactId>qiniu-java-sdk</artifactId>
    <version>7.7.0</version>
</dependency>
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>2.8.3</version>
</dependency>
<dependency>
    <groupId>com.huaweicloud</groupId>
    <artifactId>esdk-obs-java-bundle</artifactId>
    <version>3.25.5</version>
</dependency>
```

##### 1.2 配置

```yaml
na:
  oss:
    # 全局默认域名（各平台未单独配置 domain 时使用）
    domain: example.com
    # 全局最大文件大小，单位：字节（默认 10MB）
    maxSize: 1000000

    # ---------- 本地磁盘 ----------
    localPath: D:/test/          # 必填，结尾带 /
    localMaxSize: 10000000
    localWhite:
      - .doc
      - .docx

    # ---------- 七牛云 ----------
    qiNiuAccessKey: XXXXX
    qiNiuSecretKey: XXXXXX
    qiNiuBucketName: dev-qa
    qiNiuDomain: qiniudev.XXXX.com
    qiNiuMaxSize: 10000000000
    qiNiuWhite:
      - .jpg
    # 上传前是否删除同路径旧文件，默认 false
    qiNiuAutoDelete: false

    # ---------- 阿里云 ----------
    aliAccessKey: XXX
    aliSecretKey: XXX
    aliEndpoint: oss-cn-beijing.aliyuncs.com
    aliBucketName: dev-qa
    aliDomain: dev-qa.oss-cn-beijing.aliyuncs.com
    aliMaxSize: 100000000000
    aliWhite:
      - .jpg

    # ---------- 华为云 ----------
    huaweiAccessKey: XXXX
    huaweiSecretKey: XXX
    huaweiEndpoint: obs.cn-north-4.myhuaweicloud.com
    huaweiBucketName: dev-qa
    # 默认 cn-north-4
    huaweiArea: cn-north-4
    huaweiDomain: dev-qa.obs.cn-north-4.myhuaweicloud.com
    huaweiMaxSize: 100000000
    huaweiWhite:
      - .jpg
```

> 说明
> - 未配置 `aliAccessKey` / `qiNiuAccessKey` / `huaweiAccessKey` 时，对应实现不会装配。
> - 平台级 `xxxMaxSize`、`xxxWhite`、`xxxDomain` 未配置时，自动回退到全局 `maxSize`、`suffixNameWhites`、`domain`。

##### 1.3 使用

```java
@Autowired
private IOssHardDiskService ossHardDiskService;
@Autowired
private IOssQiNiuService ossQiNiuService;
@Autowired
private IOssHuaweiService ossHuaweiService;
@Autowired
private IOssAliService ossAliService;

// 上传：storageFilePath 表示目录前缀，可省略（默认按日期 yyyy/MM/dd/ 存储）
@PostMapping("/upload")
public OssDto upload(@RequestParam("file") MultipartFile file) throws IOException {
    OssDto dto = new OssDto();
    dto.setUploadFile(file);
    dto.setStorageFilePath("/test");
    return ossAliService.upload(dto, null);
}

// 删除：storageFilePath 必须传文件全路径
@PostMapping("/delete")
public OssDto delete() {
    OssDto dto = new OssDto();
    dto.setStorageFilePath("test/1750482957218.jpg");
    return ossAliService.deleteByFileName(dto, null);
}
```

阿里云分片上传：

```java
OssDto dto = new OssDto();
dto.setUploadFile(file);
// partSize 为空或小于 100KB 时默认 5MB
OssDto result = ossAliService.uploadSlice(dto, null, 5 * 1024 * 1024L);
```

返回的 `OssDto.status` 为 `OssFileOptStatus`：
`DONE`（成功）、`REMOVE`（删除成功）、`QUERY_SUCCESS`（查询成功）、
`DATA_CHECK_ERROR`（数据校验错误）、`ERROR`（操作失败）。

# 【说明】自动配置

引入 `na-oss` 依赖后，`com.na.oss.config.OssAutoConfiguration` 会通过 Spring Boot 自动配置机制自动加载：

- `AutoOssConfig`（配置前缀 `na.oss`）与本地磁盘服务 `IOssHardDiskService` 始终注册；
- 阿里云 / 华为云 / 七牛云服务仅在**对应 SDK 在类路径中**且配置了对应 `xxxAccessKey` 时注册；
- 所有 Bean 均带 `@ConditionalOnMissingBean`，如需自定义实现，声明同类型 Bean 即可覆盖。

因此**无需**再配置 `@ComponentScan("com.na")`。
