# Stage 1: Build the application
FROM maven:3-amazoncorretto-24 AS build

# Define argument for specifying the LOCKER
# This is used as the maven profile to build the application
ARG LOCKER=memory

WORKDIR /app

# Copy your project files
COPY . .

# Build the application
RUN mvn clean package -DskipTests -P${LOCKER} -ntp && \
  rm -rf /app/api/target/*-javadoc.jar && \
  mkdir -p /app/build && \
  mv /app/api/target/*.jar /app/build/


# Stage 2: Run the application
FROM gcr.io/distroless/java17-debian12
WORKDIR /app
COPY --from=build /app/build/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
