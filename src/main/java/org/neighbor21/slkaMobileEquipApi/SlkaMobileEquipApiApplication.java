package org.neighbor21.slkaMobileEquipApi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * packageName    : org.neighbor21.slkaMobileEquipApi
 * fileName       : SlkaMobileEquipApiApplication.java
 * author         : kjg08
 * date           : 24. 4. 18.
 * description    : SLKA Mobile Equipment API 애플리케이션의 시작 클래스. Spring Boot 애플리케이션을 실행하고 스케줄링을 활성화합니다.
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 24. 4. 18.        kjg08           최초 생성
 */
@EnableScheduling
@SpringBootApplication
public class SlkaMobileEquipApiApplication {

    /**
     * SLKA Mobile Equipment API 애플리케이션의 진입점입니다.
     *
     * @param args 애플리케이션 시작 시 전달되는 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(SlkaMobileEquipApiApplication.class, args);
        System.out.println("SLKA Mobile Equipment API 애플리케이션이 시작되었습니다.");
    }
}