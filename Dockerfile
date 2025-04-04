FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.authors="rollw"
LABEL org.opencontainers.image.title="Lampray"
LABEL org.opencontainers.image.url="github.com/roll-w/lampray"
LABEL org.opencontainers.image.license="Apache-2.0"
LABEL org.opencontainers.image.description="Lampray is a blog system based on Spring Boot 3 framework."

ARG LAMPRAY_VERSION="0.1.0"
ARG CTX_PATH="./build/dist/"

ADD ${CTX_PATH}lampray-${LAMPRAY_VERSION}-dist.tar.gz /app
RUN mv /app/lampray-${LAMPRAY_VERSION} /app/lampray

ENV PATH="/app/lampray/bin:$PATH"
WORKDIR /app/lampray

EXPOSE 5100 5101

# TODO: support args
ENTRYPOINT ["/app/lampray/bin/lampray"] CMD [""]
