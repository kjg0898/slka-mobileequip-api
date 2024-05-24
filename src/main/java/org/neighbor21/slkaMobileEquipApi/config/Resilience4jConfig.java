package org.neighbor21.slkaMobileEquipApi.config;

import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.config
 * fileName       : Resilience4jConfig.java
 * author         : kjg08
 * date           : 2024-05-10
 * description    : Resilience4j를 이용한 재시도 구성을 위한 설정 파일. 이 설정은 서비스 호출 실패 시 지연을 두고 재시도하는 로직을 정의합니다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2024-05-10        kjg08           최초 생성
 */

@Configuration // 이 클래스가 스프링 설정 클래스임을 나타냄
public class Resilience4jConfig {

    /**
     * 기본 재시도 구성을 정의하는 빈을 생성.
     * 최대 재시도 횟수와 재시도 간 대기 시간을 설정하며, 모든 예외에 대해 재시도함.
     *
     * @return RetryConfig 객체
     */
    @Bean
    public RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(1) // 최대 재시도 횟수
                .waitDuration(java.time.Duration.ofMillis(2000)) // 각 재시도 사이의 대기 시간
                .retryExceptions(Exception.class) // 재시도할 예외 유형
                .build();
    }
}