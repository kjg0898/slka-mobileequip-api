@echo off
SETLOCAL

REM 환경 변수 설정
SET DOCKER_USERNAME=deployment
SET DOCKER_PASSWORD=!deployment123
SET DOCKER_REGISTRY=124.43.79.169:5000

REM 경로 설정
SET PROJECT_PATH=%~dp0
SET DOCKER_IMAGE=%DOCKER_REGISTRY%/srlk/neighbor/srlk-mobileequip-api:0.1

REM Docker 데몬 실행 확인
echo Checking Docker daemon...
docker info
IF %ERRORLEVEL% NEQ 0 (
    echo Docker daemon is not running. Please start Docker Desktop.
    PAUSE
    EXIT /B 1
)

REM Docker 이미지 생성
echo Building Docker image...
docker build -t %DOCKER_IMAGE% %PROJECT_PATH%
IF %ERRORLEVEL% NEQ 0 (
    echo Docker image build failed.
    PAUSE
    EXIT /B 1
)

REM Docker 로그인
echo Logging in to Docker...
docker login %DOCKER_REGISTRY% -u %DOCKER_USERNAME% -p %DOCKER_PASSWORD%
IF %ERRORLEVEL% NEQ 0 (
    echo Docker login failed.
    PAUSE
    EXIT /B 1
)

REM Docker 이미지 푸시
echo Pushing Docker image...
docker push %DOCKER_IMAGE%
IF %ERRORLEVEL% NEQ 0 (
    echo Docker image push failed.
    PAUSE
    EXIT /B 1
)

REM 스크립트 완료 메시지
echo Docker image build and push completed successfully.

ENDLOCAL
@echo on
PAUSE
