package org.neighbor21.slkaMobileEquipApi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.config
 * fileName       : ExecutorConfig.java
 * author         : kjg08
 * date           : 24. 7. 8.
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 7. 8.        kjg08           최초 생성
 */
@Configuration
public class ExecutorConfig {

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(10);
    }
}