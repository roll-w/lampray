FROM eclipse-temurin:21-jre-alpine as builder

ARG LAMPRAY_VERSION="0.1.0"
ARG CTX_PATH="./build/dist/"

ADD ${CTX_PATH}lampray-${LAMPRAY_VERSION}-dist.tar.gz /app
RUN mv /app/lampray-${LAMPRAY_VERSION} /app/lampray

FROM eclipse-temurin:21-jre-alpine

# OCI standard labels
LABEL org.opencontainers.image.created="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
LABEL org.opencontainers.image.authors="rollw"
LABEL org.opencontainers.image.title="Lampray"
LABEL org.opencontainers.image.description="Lampray is a blog system based on Spring Boot 3 framework."
LABEL org.opencontainers.image.url="https://github.com/roll-w/lampray"
LABEL org.opencontainers.image.source="https://github.com/roll-w/lampray"
LABEL org.opencontainers.image.version="${LAMPRAY_VERSION}"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.vendor="RollW"
LABEL org.opencontainers.image.documentation="https://github.com/roll-w/lampray/blob/main/README.md"

# Create non-root user for security
RUN addgroup -g 1001 lampray && \
    adduser -D -u 1001 -G lampray lampray

COPY --from=builder --chown=lampray:lampray /app/lampray /app/lampray

USER lampray

ENV PATH="/app/lampray/bin:$PATH"
WORKDIR /app/lampray

EXPOSE 5100 5101

ENTRYPOINT ["/bin/sh", "-c", "/app/lampray/bin/lampray"]
CMD [""]
