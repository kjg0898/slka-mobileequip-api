package org.neighbor21.slkaMobileEquipApi.config;

import jakarta.annotation.PostConstruct;
import kong.unirest.Unirest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi.config
 * fileName       : UnirestConfig.java
 * author         : kjg08
 * date           : 24. 5. 10.
 * description    : Unirest Configuration 분리
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 5. 10.        kjg08           최초 생성
 */
@Configuration
public class UnirestConfig {

    @Value("${api.timeout.connect}")
    private int connectTimeout;

    @Value("${api.timeout.socket}")
    private int socketTimeout;

    @PostConstruct
    public void init() {
        Unirest.config()
                .connectTimeout(connectTimeout)
                .socketTimeout(socketTimeout);
    }
}
