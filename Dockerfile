# Stage 1: Build the application
FROM ghcr.io/graalvm/graalvm-community:23 as build

# Install Maven needed for building the application
RUN microdnf install -y maven

# Define argument for specifying the LOCKER
ARG LOCKER=memory

WORKDIR /app

# Copy your project files
COPY . .

# Build the application using Maven and GraalVM
RUN mvn clean package -P${LOCKER} -Pnative -DskipTests -Dmaven.javadoc.skip=true -ntp 

# Stage 2: Create a lightweight runtime environment
FROM ghcr.io/graalvm/graalvm-community:23

# Set the working directory
WORKDIR /app

# Copy only the final binary from the build stage
COPY --from=build /app/api/target/api .

# Ensure the binary has execute permissions
RUN chmod +x /app/api

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["/app/api"]]