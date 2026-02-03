package com.postzen.service;

import com.postzen.dto.response.FileUploadResponse;
import com.postzen.entity.UploadedFile;
import com.postzen.exception.BadRequestException;
import com.postzen.exception.ResourceNotFoundException;
import com.postzen.repository.UploadedFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final UploadedFileRepository uploadedFileRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public FileUploadResponse uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Invalid file type. Allowed: JPEG, PNG, GIF, WebP");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            UploadedFile uploadedFile = UploadedFile.builder()
                    .filename(newFilename)
                    .originalName(originalFilename)
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();

            uploadedFile = uploadedFileRepository.save(uploadedFile);
            log.info("File uploaded: {}", newFilename);

            return FileUploadResponse.of(
                    uploadedFile.getId(),
                    newFilename,
                    originalFilename,
                    contentType,
                    file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Could not store file", e);
        }
    }

    public Resource getFile(String filename) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
        } catch (IOException e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    public String getContentType(String filename) {
        return uploadedFileRepository.findByFilename(filename)
                .map(UploadedFile::getContentType)
                .orElse("application/octet-stream");
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }
}
