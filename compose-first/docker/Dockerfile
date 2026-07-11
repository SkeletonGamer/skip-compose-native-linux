# POC 2: Linux (arm64) container running the CMP app on a REAL X server (Xvfb virtual framebuffer),
# not offscreen. Captures the framebuffer and drives clicks with xdotool. This is surface 2, the judge.
#
#   docker build -f docker/Dockerfile -t poc2-linux .
#   docker run --rm -v "$PWD/docker/out:/out" poc2-linux
FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y --no-install-recommends \
        curl unzip \
        xvfb x11-utils xdotool imagemagick \
        fontconfig libfreetype6 fonts-dejavu-core \
        libgl1 libx11-6 libxext6 libxrender1 libxi6 libxtst6 \
    && rm -rf /var/lib/apt/lists/*

ENV GRADLE_VERSION=9.6.1
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/g.zip \
    && unzip -q /tmp/g.zip -d /opt && rm /tmp/g.zip
ENV PATH="/opt/gradle-${GRADLE_VERSION}/bin:${PATH}"

# Skia software backend (no GPU in the container); Xvfb provides the display server.
ENV SKIKO_RENDER_API=SOFTWARE

RUN mkdir -p /out
WORKDIR /app/compose-first
COPY . /app/compose-first
RUN gradle --no-daemon compileKotlin || true

CMD ["bash", "docker/run-xvfb.sh"]
