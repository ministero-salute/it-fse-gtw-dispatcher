FROM paas-base-image/openjdk-21:1.21-3.1737580424

WORKDIR /workspace/app

ARG JAR_FILE=./*.jar
ARG RUNTIME=./runtime

ENV AB_JOLOKIA_OFF=true
ENV WORKBENCH_MAX_METASPACE_SIZE=256

ENV JAVA_OPTIONS="-Xms256m \
    -Xmx640m \
    -XX:MetaspaceSize=128m \
    -XX:MaxMetaspaceSize=256m \
    -XX:MaxDirectMemorySize=64m \
    -XX:ReservedCodeCacheSize=64m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+ParallelRefProcEnabled \
    -XX:+UseCompressedOops \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError"

COPY ${JAR_FILE} /deployments/
COPY ${RUNTIME} /deployments/

USER jboss