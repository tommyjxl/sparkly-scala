# sparkly-scala
Demo Spark application for Town of Utopia problem

## Architecture overview
todo

## Local environment setup (Windows)
Download and install the latest stable releases of the following:
- [Java Developer Kit 11 (JDK)](https://www.oracle.com/sg/java/technologies/javase/jdk11-archive-downloads.html)
  - Note: Do not use a later version! Version 11 is used to bypass this permission error specific to creating a spark context on Windows: `java.lang.UnsupportedOperationException: getSubject is supported only if a security manager is allowed` (ref: [StackOverflow reference](https://stackoverflow.com/a/79017758))
  - Set `JAVA_HOME` env var accordingly
  - Verify that `java -version` is as expected
- [Scala](https://www.scala-lang.org/download/)
  - Verify that `scala -version` is as expected
- [Apache Spark](https://spark.apache.org/downloads.html)
  - _For package type, choose "Pre-built for Apache Hadoop \<major\>.\<minor\> and later (Scala \<major\>.\<minor\>)"_
  - Set `SPARK_HOME` env var based on the parent dir of the `bin` directory
  - On Windows, to allow Hadoop compatibility, install [winutils](https://github.com/cdarlint/winutils)
  - Set `HADOOP_HOME` env var based on the bin directory containing `hadoop.dll` and `winutils.exe`
  - Verify that `spark-shell` runs without errors
- [IntelliJ Community Edition](https://www.jetbrains.com/idea/)
  - Set JDK path if prompted
  - Install Scala plugin if prompted
  - Install Scala SDK if prompted (Match the same version as the scala version installed)
- [Docker desktop](https://docs.docker.com/desktop/)
  - Verify that `docker version` is as expected
  - In IntelliJ, install Docker plugin if prompted

## Running the scala application
### Local (Windows)
- Compile
  - Using `scalac`: `scalac -cp "C:\spark\jars\*" src/main/scala/UtopiaApp.scala -d target/scala-2.12/utopia-app-assembly-1.0.jar`
  - Using `sbt`:
    - `sbt clean`
    - `sbt compile`
    - `sbt package`
    - `sbt assembly`
- Run: `spark-submit --class UtopiaApp --master local[*] <path/to/jar>`

### Containerized
- Run Docker Desktop
- Build: `docker build --progress=plain -t utopia-app:latest .`
- Run: `docker run --rm utopia-app:latest`

### Cloud (GCP)
- Option A ([ref](https://cloud.google.com/dataproc/docs/tutorials/spark-scala)):
  - Compile jar file
  - Upload jar file to GCS (Cloud storage)
  - Submit Spark job to Dataproc on GKE
- Option B ([ref](https://jaceklaskowski.github.io/spark-kubernetes-book/demo/deploying-spark-application-to-google-kubernetes-engine/#pushing-image-to-container-registry))
  - Build docker image
  - Upload image to container registry
  - Create GKE (Google Kubernetes Engine) cluster
  - Deploy to GKE
