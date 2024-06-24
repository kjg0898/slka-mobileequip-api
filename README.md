# SLKA Mobile Equipment API 프로젝트 설명서

## 개요
SLKA Mobile Equipment API 프로젝트는 이동형 장비의 설치 위치와 통과 차량 데이터를 관리하는 시스템입니다. 이 프로젝트는 데이터를 수집, 처리, 저장하는 기능을 제공하며, 예약된 작업을 통해 주기적으로 데이터를 갱신합니다. 이 문서는 프로젝트의 전체적인 흐름, 사용된 기술, 주요 클래스 및 메서드, 그리고 외부 라이브러리에 대한 설명을 포함합니다.

## 목차
1. [프로젝트 구조](#프로젝트-구조)
2. [사용 기술 및 라이브러리](#사용-기술-및-라이브러리)
3. [주요 구성 요소](#주요-구성-요소)
4. [코드 설명](#코드-설명)
5. [데이터 흐름](#데이터-흐름)
6. [결론](#결론)

## 프로젝트 구조
org.neighbor21.slkaMobileEquipApi
├── config
│ ├── Constants.java
│ └── UnirestConfig.java
├── dto
│ ├── individualVehicles
│ │ └── IndividualVehiclesDTO.java
│ ├── listSite
│ │ ├── ClassificationDTO.java
│ │ ├── ListSiteDTO.java
│ │ └── SurveyPeriodDTO.java
├── entity
│ ├── TL_MVMNEQ_CUREntity.java
│ ├── TL_MVMNEQ_LOGEntity.java
│ ├── TL_MVMNEQ_PASSEntity.java
│ ├── TL_MVMNEQ_PERIODEntity.java
│ ├── compositeKey
│ │ ├── TL_MVMNEQ_CUR_IdEntity.java
│ │ ├── TL_MVMNEQ_LOG_IdEntity.java
│ │ ├── TL_MVMNEQ_PASS_IdEntity.java
│ │ └── TL_MVMNEQ_PERIOD_IdEntity.java
├── handler
│ └── ScheduledTasksHandler.java
├── jpaRepository
│ ├── TL_MVMNEQ_CURRepository.java
│ ├── TL_MVMNEQ_LOGRepository.java
│ ├── TL_MVMNEQ_PASSRepository.java
│ └── TL_MVMNEQ_PERIODRepository.java
├── service
│ ├── BatchService.java
│ ├── MCATLYSTApiService.java
│ ├── conversion
│ │ ├── SiteService.java
│ │ ├── SurveyPeriodService.java
│ │ └── VehiclePassService.java
│ ├── log
│ │ ├── HeaderInfo.java
│ │ └── LogService.java
│ └── util
│ └── VehicleUtils.java
└── SlkaMobileEquipApiApplication.java

## 사용 기술 및 라이브러리

### 주요 기술
- **Spring Boot**: 애플리케이션 구성을 간단하게 하고, 필요한 종속성을 자동으로 관리합니다.
- **Spring Data JPA**:  기반 데이터 접근 추상화 프레임워크로 JPA를 지원. 다양한 데이터 소스에 대한 접근을 간소화
- **Hibernate**: ORM 프레임워크로, 객체 지향 프로그래밍과 관계형 데이터베이스 간의 맵핑을 처리합니다.
- **NativeSQL**  JPA에서 JPQL 대신 직접 SQL을 사용하는 방법
- **Unirest**: HTTP 요청을 간단하게 보낼 수 있게 해주는 라이브러리입니다.
- **Lombok**: 반복적인 코드를 줄이기 위해 사용됩니다.
- **Jackson**: JSON 데이터를 Java 객체로 변환하고, 그 반대로 변환하는 데 사용됩니다.
- **PostgreSQL**: 데이터베이스 관리 시스템으로 사용됩니다.
- **Logback**: 로깅 프레임워크입니다.
- **Resilience4j**: 회복 탄력성 패턴을 제공하는 라이브러리로, 재시도 로직을 구현하는 데 사용됩니다.

### 외부 라이브러리
- **Lombok**: 반복적인 getter, setter, 생성자 등을 자동으로 생성해주는 라이브러리입니다.
- **Unirest**: HTTP 클라이언트 라이브러리입니다.
- **Jackson Databind**: JSON 데이터를 Java 객체로 변환하고, 그 반대로 변환합니다.
- **Logback Classic**: 로깅을 위한 라이브러리입니다.
- **QueryDSL JPA**: QueryDSL, JOOQ: 비표준 오픈소스 프레임워크로, 각각 JPQL 및 SQL 빌더로 사용됨. 여기서는 jqpl
- **PostgreSQL**: PostgreSQL 데이터베이스와의 상호작용을 위해 사용됩니다.
- **Resilience4j Core**: 회복 탄력성 패턴을 제공하는 라이브러리입니다.

## 주요 구성 요소

### 설정 파일 및 클래스
- **application.properties**: 애플리케이션의 설정을 정의합니다. 데이터베이스 설정, JPA 설정, API URL 및 키, 스케줄러 설정 등이 포함됩니다.
- **Constants.java**: 애플리케이션에서 사용되는 상수 값을 정의합니다.
- **UnirestConfig.java**: Unirest 설정을 관리하는 클래스입니다. API 호출 시의 연결 및 소켓 타임아웃을 설정합니다.

### DTO (Data Transfer Objects)
- **IndividualVehiclesDTO.java**: 개별 차량 데이터를 전송하기 위한 DTO입니다.
- **ListSiteDTO.java**: 장소 목록 데이터를 전송하기 위한 DTO입니다.

### 엔티티 및 복합 키
- **TL_MVMNEQ_CUREntity.java**: 이동형 장비 설치 위치 관리 엔티티입니다.
- **TL_MVMNEQ_LOGEntity.java**: 이동형 장비 설치 위치 이력 관리 엔티티입니다.
- - **TL_MVMNEQ_PASSEntity.java**: 이동형장비 통과차량 정보 이력 관리 엔티티입니다.
- - **TL_MVMNEQ_PERIODEntity.java**: 이동형장비 조사기간 정보 이력 관리 엔티티입니다.

### 서비스 클래스
- **MCATLYSTApiService.java**: Metrocount API를 호출하여 데이터를 가져오고, DTO에 저장합니다.
- **BatchService.java**: 배치처리.
  
### 스케줄러
- **ScheduledTasksHandler.java**: 예약된 작업을 처리하는 핸들러 클래스입니다. 주기적으로 API 데이터를 가져와서 처리합니다.

### JPA 리포지토리
- **TL_MVMNEQ_CURRepository.java**: TL_MVMNEQ_CUR 테이블에 접근하는 JPA 리포지토리입니다.
- **TL_MVMNEQ_LOGRepository.java**: TL_MVMNEQ_LOG 테이블에 접근하는 JPA 리포지토리입니다.
- **TL_MVMNEQ_PASSRepository.java**: TL_MVMNEQ_PASS 테이블에 접근하는 JPA 리포지토리입니다.
- **TL_MVMNEQ_PERIODRepository.java**: TL_MVMNEQ_PERIOD 테이블에 접근하는 JPA 리포지토리입니다.

## 코드 설명

### 설정 파일 (application.properties)
- **데이터베이스 설정**: 데이터베이스 URL, 사용자 이름, 비밀번호, 드라이버 클래스 이름 등을 정의합니다.
- **JPA 설정**: Hibernate의 DDL 자동 설정, SQL 방언, SQL 출력 형식 등을 정의합니다.
- **HikariCP 설정**: 데이터베이스 커넥션 풀 설정을 정의합니다.
- **API URL 및 키 설정**: 외부 API 호출에 사용되는 URL 및 키를 정의합니다.
- **스케줄러 설정**: 예약된 작업의 실행 주기를 정의합니다.

### 주요 클래스 및 메서드 설명
- **MCATLYSTApiService.java**
    - `listSites()`: List Sites API를 호출하여 장소 목록 데이터를 가져옵니다.
    - `individualVehicles(Integer siteId)`: Individual Vehicles API를 호출하여 개별 차량 데이터를 가져옵니다.
    - `cacheSite(Integer siteId)`: 사이트 ID를 캐시에 저장합니다.
    - `isCacheEmpty()`: 캐시가 비어있는지 확인합니다.
    - `getSiteCache()`: 캐시에 저장된 사이트 ID를 반환합니다.
- **ScheduledTasksHandler.java**
    - `init()`: 초기화 메서드로, 마지막 차량 통과 시간을 로드합니다.
    - `fetchAndCacheListSite()`: ?시간마다 List Sites API를 호출하여 장소 목록 데이터를 갱신합니다.
    - `fetchIndividualVehicles()`: ?시간 간격으로 Individual Vehicles API를 호출하여 개별 차량 데이터를 처리합니다.

## 데이터 흐름

### List Sites 데이터 수집
1. `ScheduledTasksHandler`의 `fetchAndCacheListSite()` 메서드가 ?시간마다 실행됩니다.
2. `MCATLYSTApiService`의 `listSites()` 메서드를 호출하여 List Sites API로부터 데이터를 가져옵니다.
3. 가져온 데이터를 `SiteService`를 통해 데이터베이스에 저장합니다.

### Individual Vehicles 데이터 수집
1. `ScheduledTasksHandler`의 `fetchIndividualVehicles()` 메서드가 ?시간 간격으로 실행됩니다.
2. `MCATLYSTApiService`의 `individualVehicles(Integer siteId)` 메서드를 호출하여 Individual Vehicles API로부터 데이터를 가져옵니다.
3. 가져온 데이터를 `VehiclePassService`를 통해 데이터베이스에 저장합니다.

### 데이터 저장
- `SiteService`는 장소 목록 데이터를 `TL_MVMNEQ_CUREntity`와 `TL_MVMNEQ_LOGEntity`로 변환하여 저장합니다.
- `VehiclePassService`는 개별 차량 데이터를 `TL_MVMNEQ_PASSEntity`로 변환하여 저장합니다.

