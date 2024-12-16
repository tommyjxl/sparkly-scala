FROM openjdk:11-jdk-slim

ENV SPARK_VERSION=3.5.3
ENV SCALA_VERSION=2.12.18
ENV HADOOP_VERSION=3

ENV UTOPIA_TEMP_DIR="/opt/tmp"
ENV UTOPIA_ARTIFACTS_DIR="/opt/artifacts"

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

# Run the application
#COPY src2/SimpleWordCount.jar /opt/spark/SimpleWordCount.jar
#COPY src2/SimpleWordCount.jar /opt/SimpleWordCount.jar
#COPY src2/SimpleWordCount.jar /SimpleWordCount.jar

#RUN ls /opt/

COPY src /opt/src
COPY artifacts ${UTOPIA_ARTIFACTS_DIR}

# Compile into jar file
RUN scalac -cp "/opt/spark/jars/*" /opt/src/main/scala/UtopiaApp.scala -d UtopiaApp.jar

RUN ls /opt/
# Run the Spark app
CMD ["spark-submit", "--class", "UtopiaApp", "UtopiaApp.jar"]

