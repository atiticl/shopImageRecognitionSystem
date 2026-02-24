package com.shopimage.service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {
    private final MinioClient minioClient;
    @Value("${minio.bucket}")
    private String bucket;
    @Value("${minio.endpoint}")
    private String endpoint;

    public String getBucket(){
        return bucket;
    }

    public String upload(String objectName, MultipartFile file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .contentType(file.getContentType())
                            .stream(in, file.getSize(), -1)
                            .build());
        }
        // 返回minio://格式的URL，保持存储格式一致
        return "minio://" + objectName;
    }

    public String upload(String objectName, InputStream inputStream, long size, String contentType) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .contentType(contentType)
                        .stream(inputStream, size, -1)
                        .build());
        // 返回minio://格式的URL，保持存储格式一致
        return "minio://" + objectName;
    }

    public InputStream getObject(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build());
    }

    public String presignedGetUrl(String objectName, int minutes) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucket)
                        .object(objectName)
                        .expiry((int) TimeUnit.MINUTES.toSeconds(minutes))
                        .build());
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) throws Exception {
        log.info("删除MinIO文件: {}", objectName);
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build());
        log.info("文件删除成功: {}", objectName);
    }
}