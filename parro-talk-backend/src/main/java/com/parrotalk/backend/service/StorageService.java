package com.parrotalk.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

/**
 * Storage Service.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
public class StorageService {

    /** Cloudinary instance */
    private Cloudinary cloudinary;

    /** Cloud name */
    @Value("${app.cloudinary.cloud_name:dummy_cloud}")
    private String cloudName;

    /** API key */
    @Value("${app.cloudinary.api_key:dummy_key}")
    private String apiKey;

    /** API secret */
    @Value("${app.cloudinary.api_secret:dummy_secret}")
    private String apiSecret;

    /**
     * Initialize Cloudinary instance.
     */
    @PostConstruct
    public void init() {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    /**
     * Store file to Cloudinary.
     * 
     * @param file        File to store
     * @param newFilename New filename
     * @return File URL
     */
    public String store(MultipartFile file, String newFilename) {
        try {
            // Check if file is empty
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }

            // Upload file to Cloudinary
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", newFilename,
                    "resource_type", "auto"));

            // Return the file url
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file to Cloudinary: " + e.getMessage(), e);
        }
    }
}
