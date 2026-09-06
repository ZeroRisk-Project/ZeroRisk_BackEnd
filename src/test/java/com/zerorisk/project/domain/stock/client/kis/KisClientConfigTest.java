package com.zerorisk.project.domain.stock.client.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.web.reactive.function.client.WebClient;

class KisClientConfigTest {

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private final List<Socket> acceptedSockets = new ArrayList<>();

    private KisHttpProperties properties(Duration responseTimeout) {
        return new KisHttpProperties(
                Duration.ofSeconds(1),
                responseTimeout,
                Duration.ofSeconds(1),
                5,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                Duration.ofSeconds(1));
    }

    @BeforeEach
    void startSilentServer() throws IOException {
        serverSocket = new ServerSocket(0);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    acceptedSockets.add(serverSocket.accept());
                } catch (IOException e) {
                    return;
                }
            }
        });
    }

    @AfterEach
    void stopSilentServer() throws IOException {
        for (Socket socket : acceptedSockets) {
            socket.close();
        }
        serverSocket.close();
        executor.shutdownNow();
    }

    @DisplayName("응답하지 않는 서버를 호출해도 응답 타임아웃 내에 실패")
    @Timeout(20)
    @Test
    void 응답하지_않는_서버를_호출해도_응답_타임아웃_내에_실패() {
        WebClient webClient = new KisClientConfig().kisWebClient(
                new KisProperties("http://localhost:" + serverSocket.getLocalPort(), "key", "secret"),
                properties(Duration.ofMillis(500)));

        long startedAt = System.currentTimeMillis();
        assertThatThrownBy(() -> webClient.get().uri("/").retrieve().bodyToMono(String.class).block())
                .isInstanceOf(Exception.class);
        long elapsed = System.currentTimeMillis() - startedAt;

        assertThat(elapsed).isLessThan(5000);
    }

    @DisplayName("종목 마스터 파일 클라이언트에도 타임아웃이 적용됨")
    @Timeout(20)
    @Test
    void 종목_마스터_파일_클라이언트에도_타임아웃이_적용됨() {
        WebClient webClient = new KisClientConfig()
                .stockMasterFileWebClient(properties(Duration.ofMillis(500)));

        long startedAt = System.currentTimeMillis();
        assertThatThrownBy(() -> webClient.get()
                .uri("http://localhost:" + serverSocket.getLocalPort())
                .retrieve()
                .bodyToMono(byte[].class)
                .block())
                .isInstanceOf(Exception.class);
        long elapsed = System.currentTimeMillis() - startedAt;

        assertThat(elapsed).isLessThan(5000);
    }
}