package org.neighbor21.slkaMobileEquipApi.config;

import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.config
 * fileName       : RetryConfiguration.java
 * author         : kjg08
 * date           : 24. 5. 10.
 * description    : 재시도 지연을 위한  Resilience4j RetryConfig 의 설정 파일
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 10.        kjg08           최초 생성
 */
@Configuration
public class Resilience4jConfig {

    @Bean
    public RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(java.time.Duration.ofMillis(200))
                .retryExceptions(Exception.class)
                .build();
    }
}