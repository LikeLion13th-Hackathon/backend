# 1. OpenJDK 17을 기반 이미지로 사용
FROM openjdk:17-jdk

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 JAR 파일을 컨테이너에 복사
COPY build/libs/*.jar app.jar
# COPY target/*-SNAPSHOT.jar /app/app.jar

# COPY build/libs/*.jar .
# RUN mv *.jar app.jar

# RUN JAR_NAME=$(find . -name "*.jar" | head -n 1) && mv "$JAR_NAME" app.jar

# 4. application.yml을 위한 리소스 디렉토리 생성
RUN mkdir -p /app/src/main/resources

# 5. GitHub Actions에서 전달받는 application.yml 내용을 받을 ARG 선언
ARG APPLICATION_YAML

# 6. ARG를 실제 파일로 생성
RUN echo "$APPLICATION_YAML" > /app/src/main/resources/application.yml

# 7. 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
