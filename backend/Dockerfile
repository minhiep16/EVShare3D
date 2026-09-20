# ===================================================================
# EVShare 3D Backend - Production Container Image
# Multi-stage or Pre-built JAR packaging
# ===================================================================
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Install curl for healthcheck & create unprivileged runtime user
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 -g root evshare

USER evshare

# Copy verified Spring Boot executable JAR
ARG JAR_FILE=target/evshare-backend-0.0.1-SNAPSHOT.jar
COPY --chown=evshare:root ${JAR_FILE} app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
