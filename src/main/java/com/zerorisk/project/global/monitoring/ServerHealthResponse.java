package com.zerorisk.project.global.monitoring;

public record ServerHealthResponse(
        boolean webServerUp,
        boolean databaseUp) {
}
