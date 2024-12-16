# sparkly-scala
Demo Spark application in Scala

## Architecture overview
todo

## Local environment overview (Windows 11)
Download and install the latest stable releases of the following:
- [Java Developer Kit 11 (JDK)](https://www.oracle.com/sg/java/technologies/javase/jdk11-archive-downloads.html)
    - Note: Do not use a later version! Version 11 is used to bypass this permission error specific to creating a spark context on Windows: `java.lang.UnsupportedOperationException: getSubject is supported only if a security manager is allowed` (ref: [StackOverflow reference](https://stackoverflow.com/a/79017758))
    - Set `JAVA_HOME` env var accordingly
    - Verify that `java --version` is as expected
- [Scala](https://www.scala-lang.org/download/)
    - Verify that `scala version` is as expected
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
    - Install Docker plugin if prompted

## Running the scala application
### Native
- Compile: `scalac -cp "C:\spark\jars\*" SimpleWordCount.scala -d SimpleWordCount.jar`
- Run: `spark-submit --class SimpleWordCount --master local[*] --conf spark.security.manager.enabled=false SimpleWordCount.jar`

### Containerized
- Start docker desktop:
- Build: 