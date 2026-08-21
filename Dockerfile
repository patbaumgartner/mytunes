# syntax=docker/dockerfile:1

# ------------------------------------------------------------------------------------------------
# Stage 1 - build the WebAssembly image.
#
# The build needs Oracle GraalVM specifically: the browser interop API ships only as a GraalVM JDK
# module (org.graalvm.webimage.api) and the Wasm backend lives in lib/svm/tools/svm-wasm, neither of
# which exists in a stock JDK or in GraalVM CE. Both the JDK and Binaryen are pinned and checksum
# verified so a clean clone reproduces the same toolchain.
# ------------------------------------------------------------------------------------------------
FROM debian:bookworm-slim AS build

ARG GRAALVM_URL=https://download.oracle.com/graalvm/25/archive/graalvm-jdk-25.0.4_linux-x64_bin.tar.gz
ARG GRAALVM_SHA256=76007c309f821aaf435bce63162ea0395587fc77350801c81643fe7feea37276
ARG BINARYEN_VERSION=version_132
ARG BINARYEN_URL=https://github.com/WebAssembly/binaryen/releases/download/version_132/binaryen-version_132-x86_64-linux.tar.gz
ARG BINARYEN_SHA256=195ddc94f9bc89f45abdabb0b9eea86023d727ba90eac8b35b80f2544fc30572

ENV DEBIAN_FRONTEND=noninteractive \
    JAVA_HOME=/opt/graalvm \
    MAVEN_OPTS=-Xmx4g

RUN apt-get update \
    && apt-get install --no-install-recommends -y ca-certificates curl zlib1g-dev build-essential \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL "$GRAALVM_URL" -o /tmp/graalvm.tar.gz \
    && echo "$GRAALVM_SHA256  /tmp/graalvm.tar.gz" | sha256sum -c - \
    && mkdir -p "$JAVA_HOME" \
    && tar -xzf /tmp/graalvm.tar.gz -C "$JAVA_HOME" --strip-components=1 \
    && rm /tmp/graalvm.tar.gz

# Web Image assembles its output with wasm-as, which it expects on the PATH.
RUN curl -fsSL "$BINARYEN_URL" -o /tmp/binaryen.tar.gz \
    && echo "$BINARYEN_SHA256  /tmp/binaryen.tar.gz" | sha256sum -c - \
    && mkdir -p /opt/binaryen \
    && tar -xzf /tmp/binaryen.tar.gz -C /opt/binaryen --strip-components=1 \
    && rm /tmp/binaryen.tar.gz

ENV PATH="$JAVA_HOME/bin:/opt/binaryen/bin:$PATH"

WORKDIR /workspace

# Dependencies resolve in their own layer so source edits do not re-download the world.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY tools/install-webimage-api.sh tools/

# Repackages GraalVM's browser interop module as a Maven artifact. It ships only as a JDK module,
# so without this the build cannot resolve org.graalvm.webimage:webimage-api.
RUN chmod +x tools/install-webimage-api.sh && ./tools/install-webimage-api.sh

RUN ./mvnw -B -q dependency:go-offline -Dmaven.test.skip=true -DskipQualityDeps || true

COPY src/ src/
RUN ./mvnw -B -Pnative native:compile -Dmaven.test.skip=true -DskipQualityDeps

# The served site is the static shell plus the generated loader and Wasm module. Nothing else.
RUN mkdir -p /site \
    && cp -r src/main/web/. /site/ \
    && cp target/mytunes.js target/mytunes.js.wasm /site/

# ------------------------------------------------------------------------------------------------
# Stage 2 - serve the static site.
#
# There is deliberately no JVM, no Spring Boot and no application code in this image: the entire
# application is inside mytunes.js.wasm and executes in the visitor's browser.
# ------------------------------------------------------------------------------------------------
FROM nginxinc/nginx-unprivileged:1.29-alpine AS runtime

USER root
COPY --from=build /site/ /usr/share/nginx/html/
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY docker/security-headers.conf /etc/nginx/security-headers.conf
RUN printf 'ok\n' > /usr/share/nginx/html/healthz \
    && chown -R nginx:nginx /usr/share/nginx/html
USER nginx

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/healthz || exit 1
