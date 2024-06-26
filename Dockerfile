# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk

# Set environment variables
ENV TZ=Asia/Colombo
ENV APP_HOME=/usr/app/

# Create application directory
WORKDIR $APP_HOME

# Copy the jar file into the container
COPY build/libs/srlk-mobileequip-api-1.0-jar-with-dependencies.jar app.jar

## Copy the configuration files into the container
#COPY src/main/resources/application.properties .
#COPY src/main/resources/application-test.properties .
#COPY src/main/resources/application-real.properties .
#COPY src/main/resources/logback.xml .


# Run the application when the container launches
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
