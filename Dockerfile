FROM eclipse-temurin:21-jre-alpine as builder

ARG LAMPRAY_VERSION="0.1.0"
ARG CTX_PATH="./build/dist/"

ADD ${CTX_PATH}lampray-${LAMPRAY_VERSION}-dist.tar.gz /app
RUN mv /app/lampray-${LAMPRAY_VERSION} /app/lampray

FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.authors="rollw"
LABEL org.opencontainers.image.title="Lampray"
LABEL org.opencontainers.image.url="github.com/roll-w/lampray"
LABEL org.opencontainers.image.license="Apache-2.0"
LABEL org.opencontainers.image.description="Lampray is a blog system based on Spring Boot 3 framework."

COPY --from=builder /app/lampray /app/lampray

ENV PATH="/app/lampray/bin:$PATH"
WORKDIR /app/lampray

EXPOSE 5100 5101

ENTRYPOINT ["/bin/sh", "-c", "/app/lampray/bin/lampray"]
CMD [""]
