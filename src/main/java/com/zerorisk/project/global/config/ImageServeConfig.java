package com.zerorisk.project.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageServeConfig implements WebMvcConfigurer {

    @Value("${app.upload.image-dir}")
    private String imageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = imageDir.endsWith("/") ? imageDir : imageDir + "/";

        registry.addResourceHandler("/api/images/**")
                .addResourceLocations("file:" + location);
    }
}
