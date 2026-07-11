# Linux container that builds the desktop-witness and renders it OFFSCREEN with Skia
# software rendering: the honest "run on Linux" check for the PoC (step 6).
#
# Build context is the desktop-witness/ project root:
#   docker build -f docker/Dockerfile -t poc-skip-linux .
# Run, capturing PNGs to a host dir mounted at /app/docs:
#   docker run --rm -v "$PWD/docker/out:/app/docs" poc-skip-linux
# Or just use docker/render-linux.sh which does both and copies to ../docs/linux-*.png.
FROM eclipse-temurin:21-jdk

# Native libs Skia (skiko) needs for headless raster rendering + a font for text.
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl unzip fontconfig libfreetype6 fonts-dejavu-core \
        libgl1 libx11-6 libxext6 libxrender1 \
    && rm -rf /var/lib/apt/lists/*

# Gradle (same version as the macOS host).
ENV GRADLE_VERSION=9.6.1
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/g.zip \
    && unzip -q /tmp/g.zip -d /opt && rm /tmp/g.zip
ENV PATH="/opt/gradle-${GRADLE_VERSION}/bin:${PATH}"

# Force Skia's software backend (no GPU/X server in the container).
ENV SKIKO_RENDER_API=SOFTWARE

RUN mkdir -p /app/docs
WORKDIR /app/desktop-witness
COPY . /app/desktop-witness

# Warm the build (deps + compile) into an image layer.
RUN gradle --no-daemon compileKotlin || true

CMD ["gradle", "--no-daemon", "--console=plain", "renderPng"]
