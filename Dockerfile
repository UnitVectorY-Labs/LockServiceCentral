# Stage 1: Build the application
FROM maven:3-amazoncorretto-25 AS build

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
FROM gcr.io/distroless/java17-debian13

# Re-declare the ARG to make it available in this stage
ARG LOCKER=memory

# Set SPRING_PROFILES_ACTIVE based on the LOCKER used to build the image
ENV SPRING_PROFILES_ACTIVE=${LOCKER}

WORKDIR /app
COPY --from=build /app/build/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
