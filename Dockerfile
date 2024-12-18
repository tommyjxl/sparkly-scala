FROM openjdk:11-jdk-slim

ENV SPARK_VERSION=3.5.3
ENV SCALA_VERSION=2.12.18
ENV HADOOP_VERSION=3

ENV TOU_TEMP_DIR="/tmp"
ENV TOU_ARTIFACTS_DIR="/opt/artifacts"

# procps (ps command) is not included in the slim package.
# needed because ps ../load-spark-env.sh is called when running the Spark app
RUN apt-get update && apt-get install -y \
    wget \
    curl \
    procps \
    && rm -rf /var/lib/apt/lists/*

# Download and install Spark
RUN wget https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz \
    && tar -xzf spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz \
    && mv spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION} /opt/spark \
    && rm spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz

# Download and install Scala
RUN wget https://downloads.lightbend.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz \
    && tar -xzf scala-${SCALA_VERSION}.tgz \
    && mv scala-${SCALA_VERSION} /opt/scala \
    && rm scala-${SCALA_VERSION}.tgz

# Set environment variables for Scala and Spark
ENV PATH="/opt/spark/bin:/opt/scala/bin:${PATH}"

WORKDIR /opt

COPY src /opt/src
COPY artifacts ${TOU_ARTIFACTS_DIR}

# Compile into fat jar file
RUN scalac -cp "/opt/spark/jars/*" src/main/scala/com/tou/global/*.scala src/main/scala/com/tou/producer/*.scala src/main/scala/com/tou/*.scala -d tou-app.jar

RUN ls /opt/
RUN ls ${TOU_ARTIFACTS_DIR}

# Run the Spark app
CMD ["spark-submit", "--class", "com.tou.App", "--master", "local[*]", "tou-app.jar", "3", "/opt/artifacts/geolocation.parquet", "/opt/artifacts/videoCameraItemsDetected.parquet", "/opt/artifacts/videoCameraItemsDetectedByLocation.parquet", "/opt/artifacts/topItems.parquet"]
