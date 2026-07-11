# POC 3 Jalon 1: Linux arm64 container that builds a Kotlin/Native executable using the skiko
# linuxArm64 Skia klib and runs it (no JVM in the produced binary; the JVM is build-time only).
FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y --no-install-recommends \
        curl unzip git \
        libgl1-mesa-dev libglu1-mesa-dev libfontconfig1-dev libfreetype6-dev \
        libgl1 fontconfig libfreetype6 fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

ENV GRADLE_VERSION=9.6.1
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/g.zip \
    && unzip -q /tmp/g.zip -d /opt && rm /tmp/g.zip
ENV PATH="/opt/gradle-${GRADLE_VERSION}/bin:${PATH}"

RUN mkdir -p /out
WORKDIR /app/poc3-native
COPY . /app/poc3-native

# Build (downloads the Kotlin/Native toolchain to ~/.konan on first run) then run the native binary.
CMD ["bash","-lc","gradle --no-daemon linkDebugExecutableLinuxArm64 && echo '== built ==' && ls -la build/bin/linuxArm64/debugExecutable/ && ./build/bin/linuxArm64/debugExecutable/poc3-native.kexe"]
