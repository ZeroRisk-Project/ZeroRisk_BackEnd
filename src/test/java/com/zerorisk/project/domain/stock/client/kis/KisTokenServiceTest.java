package com.zerorisk.project.domain.stock.client.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class KisTokenServiceTest {

    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration TOKEN_LOCK_TIMEOUT = Duration.ofMillis(200);

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private final List<Socket> acceptedSockets = new ArrayList<>();

    @BeforeEach
    void startSilentServer() throws IOException {
        serverSocket = new ServerSocket(0);
        executor = Executors.newFixedThreadPool(3);
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

    private KisTokenService tokenService() {
        KisHttpProperties httpProperties = new KisHttpProperties(
                Duration.ofSeconds(1),
                RESPONSE_TIMEOUT,
                Duration.ofSeconds(1),
                5,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                TOKEN_LOCK_TIMEOUT);
        KisProperties kisProperties =
                new KisProperties("http://localhost:" + serverSocket.getLocalPort(), "key", "secret");

        return new KisTokenService(
                new KisClientConfig().kisWebClient(kisProperties, httpProperties),
                kisProperties,
                httpProperties);
    }

    @DisplayName("토큰 발급이 지연돼도 응답 타임아웃 내에 실패")
    @Timeout(20)
    @Test
    void 토큰_발급이_지연돼도_응답_타임아웃_내에_실패() {
        KisTokenService kisTokenService = tokenService();

        long startedAt = System.currentTimeMillis();
        assertThatThrownBy(kisTokenService::getAccessToken).isInstanceOf(Exception.class);
        long elapsed = System.currentTimeMillis() - startedAt;

        // 토큰 발급도 다른 KIS 클라이언트와 동일하게 짧게 3회 재시도하므로, 매 시도가
        // 전부 타임아웃되는 최악의 경우를 기준으로 상한을 넉넉히 잡는다.
        assertThat(elapsed).isLessThan(3 * RESPONSE_TIMEOUT.toMillis() + 3000);
    }

    @DisplayName("다른 스레드가 토큰을 발급 중이면 락 대기 시간만 기다리고 실패")
    @Timeout(30)
    @Test
    void 다른_스레드가_토큰을_발급_중이면_락_대기_시간만_기다리고_실패() throws InterruptedException {
        KisTokenService kisTokenService = tokenService();
        CountDownLatch firstCallStarted = new CountDownLatch(1);

        executor.submit(() -> {
            firstCallStarted.countDown();
            try {
                kisTokenService.getAccessToken();
            } catch (Exception ignored) {

            }
        });

        assertThat(firstCallStarted.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(300);

        long startedAt = System.currentTimeMillis();
        assertThatThrownBy(kisTokenService::getAccessToken)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대기 시간을 초과");
        long elapsed = System.currentTimeMillis() - startedAt;

        assertThat(elapsed).isLessThan(1000);
    }
}