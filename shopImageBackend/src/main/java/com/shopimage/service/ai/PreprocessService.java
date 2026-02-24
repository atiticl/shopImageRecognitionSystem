package com.shopimage.service.ai;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreprocessService {
    private final MinioClient minioClient;
    @Value("${minio.bucket}")
    private String bucket;

    public float[] preprocessImage(String bucket, String objectName) throws Exception {
        // Download image from MinIO
        log.info("Attempting to download image from bucket: {}, objectName: {}", bucket, objectName);
        try {
            InputStream imageStream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build()
            );
            log.info("Successfully downloaded image from MinIO");
            return processImageStream(imageStream);
        } catch (Exception e) {
            log.error("Failed to download image from MinIO: {}", e.getMessage());
            throw e;
        }
    }

    public float[] preprocessImageFromBytes(byte[] imageBytes) throws Exception {
        return processImageBytes(imageBytes);
    }

    private float[] processImageStream(InputStream imageStream) throws Exception {
        // 读取图像数据
        byte[] imageBytes = imageStream.readAllBytes();
        imageStream.close();
        return processImageBytes(imageBytes);
    }

    private float[] processImageBytes(byte[] imageBytes) throws Exception {
        // 使用Java内置ImageIO读取图像
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new Exception("Failed to decode image");
        }
        
        // 调整图像大小为224x224
        BufferedImage resizedImage = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, 224, 224, null);
        g2d.dispose();
        
        // 转换为float数组 [224, 224, 3] (NHWC格式)
        float[] data = new float[224 * 224 * 3];
        int index = 0;
        
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                int rgb = resizedImage.getRGB(x, y);
                // 提取RGB分量并归一化到[0,1]，然后转换到[-1,1]
                float r = ((rgb >> 16) & 0xFF) / 255.0f;
                float g = ((rgb >> 8) & 0xFF) / 255.0f;
                float b = (rgb & 0xFF) / 255.0f;
                
                // 归一化到[-1, 1]
                data[index++] = r * 2.0f - 1.0f;
                data[index++] = g * 2.0f - 1.0f;
                data[index++] = b * 2.0f - 1.0f;
            }
        }
        
        return data;
    }
}
