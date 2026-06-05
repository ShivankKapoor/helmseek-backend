# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-25 AS builder

USER root
WORKDIR /app

# Copy gradle wrapper and build scripts first for layer caching
COPY --chmod=755 gradlew ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Resolve dependencies (cached if build files unchanged)
RUN ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# Copy source and build fat JAR (skip tests — run them in CI)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ─── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM registry.access.redhat.com/ubi9/openjdk-25-runtime AS runner

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 7666

ENTRYPOINT ["java", "-jar", "app.jar"]
