package com.parrotalk.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Service
public class StorageService {
    private final Cloudinary cloudinary;

    public StorageService(
            @Value("${app.cloudinary.cloud_name:dummy_cloud}") String cloudName,
            @Value("${app.cloudinary.api_key:dummy_key}") String apiKey,
            @Value("${app.cloudinary.api_secret:dummy_secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    public String store(MultipartFile file, String newFilename) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", newFilename,
                "resource_type", "auto" // Auto detects video or audio
            ));
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file to Cloudinary: " + e.getMessage(), e);
        }
    }
}
