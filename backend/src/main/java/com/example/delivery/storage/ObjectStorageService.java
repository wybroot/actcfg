package com.example.delivery.storage;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;

@Service
public class ObjectStorageService {
    private final StorageProperties storageProperties;
    private final MinioClient minioClient;

    public ObjectStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        if (isMinioEnabled()) {
            StorageProperties.Minio minio = storageProperties.minio();
            this.minioClient = MinioClient.builder()
                    .endpoint(minio.endpoint())
                    .credentials(minio.accessKey(), minio.secretKey())
                    .build();
        } else {
            this.minioClient = null;
        }
    }

    public boolean isMinioEnabled() {
        return "minio".equalsIgnoreCase(storageProperties.type()) && storageProperties.minio() != null;
    }

    public StoredObject putPackageText(String objectName, String content, String contentType, String checksum) {
        if (!isMinioEnabled()) {
            return new StoredObject("local", objectName, objectName, checksum);
        }
        String bucket = storageProperties.minio().buckets().packages();
        putText(bucket, objectName, content, contentType);
        return new StoredObject(bucket, objectName, objectUrl(bucket, objectName), checksum);
    }

    /**
     * 上传制品文件（二进制）。MinIO 可用时写入 resources bucket，否则落地到本地 basePath/resources 目录。
     * 返回的 objectUrl 供资源版本记录 externalUrl 使用。
     */
    public StoredObject putResourceFile(String objectName, InputStream stream, long size,
                                        String contentType, String checksum) {
        if (!isMinioEnabled()) {
            return putLocalFile("resources", objectName, stream, checksum);
        }
        String bucket = storageProperties.minio().buckets().resources();
        try {
            ensureBucket(bucket);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .stream(stream, size, -1)
                    .build());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "制品文件上传失败");
        }
        return new StoredObject(bucket, objectName, objectUrl(bucket, objectName), checksum);
    }

    private StoredObject putLocalFile(String subDir, String objectName, InputStream stream, String checksum) {
        try {
            String base = storageProperties.basePath() != null ? storageProperties.basePath() : "./data/storage";
            Path target = Paths.get(base, subDir, objectName);
            Files.createDirectories(target.getParent());
            Files.copy(stream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String url = "file://" + target.toAbsolutePath().normalize();
            return new StoredObject("local", objectName, url, checksum);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "制品文件本地写入失败");
        }
    }

    /**
     * 写入部署包压缩档（二进制 ZIP）。MinIO 可用则写 packages bucket，否则落地本地。
     */
    public StoredObject putPackageArchive(String objectName, byte[] zipBytes, String checksum) {
        if (!isMinioEnabled()) {
            return putLocalFile("packages", objectName,
                    new ByteArrayInputStream(zipBytes), checksum);
        }
        String bucket = storageProperties.minio().buckets().packages();
        try {
            ensureBucket(bucket);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .contentType("application/zip")
                    .stream(new ByteArrayInputStream(zipBytes), zipBytes.length, -1)
                    .build());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "部署包写入失败");
        }
        return new StoredObject(bucket, objectName, objectUrl(bucket, objectName), checksum);
    }

    /**
     * 按存储 URL 取回对象字节。支持 file:// 本地读取和 MinIO 对象 URL。
     * 无法识别或取不到（如 internal:// 占位）时返回 null，调用方按引用处理。
     */
    public byte[] fetchObjectBytes(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            if (url.startsWith("file://")) {
                Path path = Paths.get(URLDecoder.decode(url.substring("file://".length()), StandardCharsets.UTF_8));
                return Files.exists(path) ? Files.readAllBytes(path) : null;
            }
            if (isMinioEnabled() && url.startsWith(storageProperties.minio().endpoint().replaceAll("/+$", ""))) {
                // 解析出 bucket/object
                String rest = url.substring(storageProperties.minio().endpoint().replaceAll("/+$", "").length() + 1);
                int slash = rest.indexOf('/');
                if (slash < 0) return null;
                String bucket = rest.substring(0, slash);
                String object = rest.substring(slash + 1);
                try (InputStream in = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucket).object(object).build())) {
                    return in.readAllBytes();
                }
            }
            return null; // internal:// 等占位地址，无法取回
        } catch (Exception e) {
            return null;
        }
    }

    /** 部署包保留天数（供生命周期清理使用）。 */
    public int packageRetentionDays() {
        return storageProperties.retentionDays();
    }

    /**
     * 按存储 URL 删除对象。支持 file:// 本地删除与 MinIO 对象删除。
     * 文件不存在或占位地址（internal://）视为已删除返回 true；仅在真实删除失败时返回 false。
     */
    public boolean deleteObject(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        try {
            if (url.startsWith("file://")) {
                Path path = Paths.get(URLDecoder.decode(url.substring("file://".length()), StandardCharsets.UTF_8));
                Files.deleteIfExists(path);
                return true;
            }
            if (isMinioEnabled() && url.startsWith(storageProperties.minio().endpoint().replaceAll("/+$", ""))) {
                String rest = url.substring(storageProperties.minio().endpoint().replaceAll("/+$", "").length() + 1);
                int slash = rest.indexOf('/');
                if (slash < 0) return true;
                String bucket = rest.substring(0, slash);
                String object = rest.substring(slash + 1);
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(object).build());
                return true;
            }
            return true; // internal:// 等占位地址，无物理文件可删
        } catch (Exception e) {
            return false;
        }
    }

    private void putText(String bucket, String objectName, String content, String contentType) {
        try {
            ensureBucket(bucket);
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .contentType(contentType)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .build());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "对象存储写入失败");
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String objectUrl(String bucket, String objectName) {
        String endpoint = storageProperties.minio().endpoint();
        return endpoint.replaceAll("/+$", "") + "/" + bucket + "/" + objectName;
    }
}
