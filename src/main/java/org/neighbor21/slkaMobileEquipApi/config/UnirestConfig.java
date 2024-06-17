package org.neighbor21.slkaMobileEquipApi.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.annotation.PostConstruct;
import kong.unirest.Unirest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.config
 * fileName       : UnirestConfig.java
 * author         : kjg08
 * date           : 2024-05-10
 * description    : Unirest 설정을 분리하여 관리하는 클래스. API 호출 시의 연결 및 소켓 타임아웃을 설정합니다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2024-05-10        kjg08           최초 생성
 */

@Configuration
public class UnirestConfig {

    @Value("${api.timeout.connect}") // application.properties 파일에서 connectTimeout 값을 주입받음
    private int connectTimeout;

    @Value("${api.timeout.socket}") // application.properties 파일에서 socketTimeout 값을 주입받음
    private int socketTimeout;

    /**
     * Retry 객체를 Bean으로 정의합니다.
     *
     * @return Retry 객체
     */
    @Bean
    public Retry apiRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)  // 최대 시도 횟수
                .waitDuration(Duration.ofMillis(1000))  // 시도 간 대기 시간
                .build();
        return Retry.of("apiRetry", config);
    }

    /**
     * DB 연결을 위한 Retry 객체를 Bean으로 정의합니다.
     *
     * @return Retry 객체
     */
    @Bean
    public Retry dbRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(2)  // 최대 시도 횟수
                .waitDuration(Duration.ofMillis(2000))  // 시도 간 대기 시간
                .build();
        return Retry.of("dbRetry", config);
    }

    /**
     * Unirest의 설정을 초기화하는 메소드. 연결 타임아웃과 소켓 타임아웃을 설정합니다.
     * 이 메소드는 빈이 초기화된 후에 호출됩니다.
     */
    @PostConstruct
    public void init() {
        Unirest.config()
                .connectTimeout(connectTimeout) // 연결 타임아웃 설정
                .socketTimeout(socketTimeout); // 소켓 타임아웃 설정
    }
}
