# syntax=docker/dockerfile:1
FROM node:22-bookworm-slim@sha256:c3de60bf2f9dd0ac6370e6117950ff62d6e339527e7472301c9c78a017978392 AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run check && npm test && npm run build

FROM eclipse-temurin:25-jdk-noble@sha256:589ff4cc3f71aab462e7048a47a0d10edf57fbccde3fceea2281e610bf5880b4 AS backend
WORKDIR /build
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY src/ src/
COPY --from=frontend /build/frontend/dist /build/frontend-dist
RUN --mount=type=cache,target=/root/.gradle \
    sh ./gradlew --no-daemon -PfrontendDist=/build/frontend-dist test bootJar

FROM eclipse-temurin:25-jre-noble@sha256:d9a39a23634650173f1e2bbc176227af9728587ecf0f4b62d53e9355cd7a19ab AS tool-runtime
ARG TCOFFEE_VERSION=13.45.0.4846264+really13.41.0.28bdc39+dfsg-1
ARG PRIMER3_VERSION=2.6.1-4
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       t-coffee=${TCOFFEE_VERSION} primer3=${PRIMER3_VERSION} tini curl time \
    && mkdir -p /opt/genewarrior \
    && dpkg-query -W -f='${Package}\t${Version}\t${Architecture}\n' > /opt/genewarrior/packages.tsv \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 genewarrior \
    && useradd --uid 10001 --gid 10001 --no-create-home --home-dir /tmp --shell /usr/sbin/nologin genewarrior
COPY docker/fixtures/ /opt/genewarrior/fixtures/
COPY --chmod=755 docker/smoke-tools.sh /opt/genewarrior/smoke-tools
ENV HOME=/tmp TMPDIR=/tmp TMP_4_TCOFFEE=/tmp LC_ALL=C
USER 10001:10001
WORKDIR /tmp
# Only built-in alignment methods are allowed; prove no network or MUSCLE is needed.
RUN --network=none /opt/genewarrior/smoke-tools

# Optional export of the exact distro tool sources/patches, separate from the runtime.
FROM tool-runtime AS tool-source-download
USER root
RUN sed -i 's/^Types: deb$/Types: deb deb-src/' /etc/apt/sources.list.d/ubuntu.sources \
    && apt-get update \
    && mkdir /sources \
    && cd /sources \
    && apt-get source --download-only t-coffee=${TCOFFEE_VERSION} primer3=${PRIMER3_VERSION} \
    && sha256sum * > SHA256SUMS

FROM scratch AS tool-sources
COPY --from=tool-source-download /sources/ /

FROM tool-runtime AS runtime
WORKDIR /app
COPY --from=backend /build/build/libs/genewarrior.jar /app/genewarrior.jar
ENV SPRING_PROFILES_ACTIVE=production \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+ExitOnOutOfMemoryError -Djava.io.tmpdir=/tmp"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent --max-time 4 http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["/usr/bin/tini", "-g", "--", "java", "-jar", "/app/genewarrior.jar"]
