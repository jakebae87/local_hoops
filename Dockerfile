# ✅ Java 8, Spring Boot 용 백엔드 Dockerfile
FROM openjdk:8-jdk

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# 이미지 업로드 경로 등 외부 연동 파일을 위한 디렉토리 (선택)
RUN mkdir -p /app/uploads

# 포트 오픈
EXPOSE 9000

# 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]
