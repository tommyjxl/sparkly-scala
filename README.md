# sparkly-scala
Demo application to solve the "Town of Utopia" problems, including:
- Generating two datasets:
  - Video camera data with geolocation ID
  - Geolocation mapping (ID -> geolocation names) 
- Augmenting the video camera data with geolocation names
- Finding the most common item names detected by video cameras

Main requirements:
- The application must be completely written in Scala, parametrized and generalized
- All data transformations (excluding file I/O and logging) must be done using Resilient Distributed Datasets (RDDs)
- The video camera data should be augmented with minimal time/space complexity, and shuffling of data

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
- (Optional) Spark applications in Scala are verbose by default. Set the logger level to your convenience
  - Example `log4j.properties` file set to warning level logs
    ```text
    # Set root logger level
    log4j.rootCategory=WARN, console
  
    # Console appender configuration
    log4j.appender.console=org.apache.log4j.ConsoleAppender
    log4j.appender.console.layout=org.apache.log4j.PatternLayout
    log4j.appender.console.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1} - %m%n
  
    # Optional: Set specific loggers
    log4j.logger.org.apache.spark=WARN
    log4j.logger.org.apache.hadoop=WARN
    log4j.logger.yarn=WARN
    log4j.logger.io.netty=WARN
    ```
  - Specify the logging level as part of the spark-submit conf
    - eg. `spark-submit --conf "spark.driver.extraJavaOptions=-Dlog4j.configuration=file:/C:/spark/conf/log4j.properties" --conf "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:/C:/spark/conf/log4j.properties" --class com.tou.processor.DemoApp --master local[*] tou-app.jar`

## Running the scala application
### Local (Windows)
- Navigate to repo root directory, eg. `cd C:\Users\tommy\sparkly-scala`
- Compile
  - Using `scalac`: 
    ```bash
    scalac -cp "C:\spark\jars\*" src/main/scala/com/tou/global/*.scala src/main/scala/com/tou/producer/*.scala src/main/scala/com/tou/processor/*.scala -d utopia-app.jar -explain
    ```
  - _Note: Update `C:\spark\jars\*` to the corresponding location of your spark jars, or use an env var, eg. `$env:SPARK_HOME\jars\*` for Windows Powershell_
  - Verify that `tou-app.jar` was created
  - Using `sbt`:
    ```bash
      sbt clean
      sbt compile
      sbt package
      sbt assembly
    ```
   - Verify that `target/scala-2.12/tou-app-assembly-1.0.jar` was created
- Run: `spark-submit --class com.tou.processor.DemoApp --master local[*] <path/to/jar> <topItemCount>`
  - eg. `spark-submit --class com.tou.processor.DemoApp --master local[*] target/scala-2.12/tou-app-assembly-1.0.jar 3`

### Containerized
- Run Docker Desktop
- Build: `docker build --progress=plain -t tou-app:latest .`
- Run: `docker run --rm tou-app:latest`

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
