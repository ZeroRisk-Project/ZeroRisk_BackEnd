package com.zerorisk.project.global.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ImageStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    @Value("${app.upload.image-dir}")
    private String imageDir;

    // 저장 성공 시 "/api/images/{filename}" 형태의 접근 경로를 반환
    public String store(MultipartFile file) {
        validate(file);

        try {
            Path dirPath = Paths.get(imageDir);
            Files.createDirectories(dirPath);

            String extension = extractExtension(file.getOriginalFilename());
            String storedFilename = UUID.randomUUID() + extension;
            Path targetPath = dirPath.resolve(storedFilename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "/api/images/" + storedFilename;
        } catch (IOException e) {
            log.error("이미지 저장 실패", e);
            throw new ImageStorageException("이미지 저장에 실패했습니다.");
        }
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ImageStorageException("빈 파일은 업로드할 수 없습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ImageStorageException("이미지 파일(jpeg, png, gif, webp)만 업로드할 수 있습니다.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }

        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}
