### oss模块

```text
本版本发布时间为 2021-05-01  适配jdk版本为 1.8
```

#### 1 配置
##### 1.1 添加依赖
```
<dependency>
    <groupId>com.na</groupId>
    <artifactId>na-oss</artifactId>
    <version>1.0.0</version>
</dependency>
        
或者

<dependency>
    <groupId>com.na</groupId>
    <artifactId>na-oss</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../lib/na-oss-1.0.0.jar</systemPath>
</dependency>

相关依赖

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
    domain: d
    # 1M
    maxSize: 1000000
    localPath: D:/test/
    localMaxSize: 10000000
    localWhite:
      - .doc
      - .docx

    qiNiuAccessKey: XXXXX
    qiNiuSecretKey: XXXXXX
    qiNiuBucketName: dev-qa
    qiNiuDomain: qiniudev.XXXX.com
    qiNiuMaxSize: 10000000000
    qiNiuWhite:
      - .jpg

    aliAccessKey: XXX
    aliSecretKey: XXX
    aliEndpoint: oss-cn-beijing.aliyuncs.com
    aliBucketName: dev-qa
    aliDomain: dev-qa.oss-cn-beijing.aliyuncs.com
    aliMaxSize: 100000000000
    aliWhite:
      - .jpg

    huaweiAccessKey: XXXX
    huaweiSecretKey: XXX
    huaweiEndpoint: obs.cn-north-4.myhuaweicloud.com
    huaweiBucketName: dev-qa
    # 默认 cn-north-4
    #    huaweiArea:
    huaweiDomain: dev-qa.obs.cn-north-4.myhuaweicloud.com
    huaweiMaxSize: 100000000
    huaweiWhite:
      - .jpg

```

##### 1.3 使用
```java
@Autowired
private INaOssQiNiuService naOssQiNiuService;
@Autowired
private INaOssHuaweiService naOssHuaweiService;
@Autowired
private INaOssAliService naOssAliService;


//    @PostMapping("/test")
//    @NaAnonymousAccess
//    public R test(@RequestParam("file") MultipartFile file) {
//        NaOssDto dto = new NaOssDto();
//        dto.setUploadFile(file);
//        // 可以不写 默认时间路径
//        dto.setStorageFilePath("/test");
//        NaOssDto upload = naOssHardDiskService.upload(dto, null);
//        return R.success(upload);
//    }

//    @PostMapping("/test")
//    @NaAnonymousAccess
//    public R test(@RequestParam("file") MultipartFile file) {
//        NaOssDto dto = new NaOssDto();
//        dto.setStorageFilePath("test/1750476690976.doc");
//        NaOssDto upload = naOssHardDiskService.delete(dto, null);
//        return R.success(upload);
//    }


//    @PostMapping("/test")
//    @NaAnonymousAccess
//    public R test(@RequestParam("file") MultipartFile file) {
//        NaOssDto dto = new NaOssDto();
//        dto.setUploadFile(file);
//        // 可以不写 默认时间路径
//        dto.setStorageFilePath("/test");
//        NaOssDto upload = naOssQiNiuService.upload(dto, null);
//        return R.success(upload);
//    }

//    @PostMapping("/test")
//    @NaAnonymousAccess
//    public R test(@RequestParam("file") MultipartFile file) {
//        NaOssDto dto = new NaOssDto();
//        dto.setStorageFilePath("test/1750481772570.jpg");
//        NaOssDto upload = naOssQiNiuService.delete(dto, null);
//        return R.success(upload);
//    }


//    @PostMapping("/test")
//    @NaAnonymousAccess
//    public R test(@RequestParam("file") MultipartFile file) {
//        NaOssDto dto = new NaOssDto();
//        dto.setUploadFile(file);
//        // 可以不写 默认时间路径
//        dto.setStorageFilePath("/test");
//        NaOssDto upload = naOssHuaweiService.upload(dto, null);
//        return R.success(upload);
//    }

//    @PostMapping("/test")
//    @NaAnonymousAccess
//    public R test(@RequestParam("file") MultipartFile file) {
//        NaOssDto dto = new NaOssDto();
//        dto.setStorageFilePath("test/1750482057222.jpg");
//        NaOssDto upload = naOssHuaweiService.delete(dto, null);
//        return R.success(upload);
//    }

//    @PostMapping("/test")
//    @NaAnonymousAccess
//    public R test(@RequestParam("file") MultipartFile file) {
//        NaOssDto dto = new NaOssDto();
//        dto.setUploadFile(file);
//        // 可以不写 默认时间路径
//        dto.setStorageFilePath("/test");
//        NaOssDto upload = naOssAliService.upload(dto, null);
//        return R.success(upload);
//    }

@PostMapping("/test")
@NaAnonymousAccess
public R test(@RequestParam("file") MultipartFile file) {
    NaOssDto dto = new NaOssDto();
    dto.setStorageFilePath("test/1750482957218.jpg");
    NaOssDto upload = naOssHuaweiService.delete(dto, null);
    return R.success(upload);
}

```


# 【注意】启动类配置
```
如果你的包名不是以com.na开头的，需要配置
@ComponentScan(basePackages = {"com.na", "com.ziji.baoming"}) // 扫描多个包路径
```
