package com.example.delivery.storage;

import com.example.delivery.common.api.ErrorCode;
import com.example.delivery.common.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
