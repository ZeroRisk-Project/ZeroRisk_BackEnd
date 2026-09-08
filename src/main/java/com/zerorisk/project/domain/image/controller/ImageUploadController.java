package com.zerorisk.project.domain.image.controller;

import com.zerorisk.project.global.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageStorageService imageStorageService;

    @PostMapping
    public ResponseEntity<ImageUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        String imageUrl = imageStorageService.store(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ImageUploadResponse(imageUrl));
    }

    public record ImageUploadResponse(String imageUrl) {
    }
}
