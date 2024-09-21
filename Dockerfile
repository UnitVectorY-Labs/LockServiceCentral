# Stage 1: Build the application
FROM maven:3.9-amazoncorretto-17 AS build

# Define argument for specifying the DATAMODEL
# This is used as the maven profile to build the application
ARG DATAMODEL=memory

WORKDIR /app

# Copy your project files
COPY . .

# Build the application
RUN mvn clean package -DskipTests -P${DATAMODEL} -ntp && \
  rm -rf /app/api/target/*-javadoc.jar && \
  mkdir -p /app/build && \
  mv /app/api/target/*.jar /app/build/


# Stage 2: Run the application
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY --from=build /app/build/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]