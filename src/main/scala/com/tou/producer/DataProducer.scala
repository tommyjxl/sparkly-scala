package com.tou.producer

import com.tou.global.Schema
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types._
import org.apache.spark.sql.{AnalysisException, DataFrame, Row, SparkSession}

import scala.util.Random

class DataProducer(
    spark: SparkSession,
    randomSeed: Long = System.currentTimeMillis()
) {
  private val random = new Random(randomSeed)

  def dfToParquet(df: DataFrame, filePath: String): Unit = {
    df.write.mode("overwrite").parquet(filePath)
    println(s"DataFrame written to Parquet file at: $filePath")
  }

  private def dfFromParquet(filePath: String, schema: StructType): DataFrame = {
    val df = spark.read.parquet(filePath)
    println(s"DataFrame read from Parquet file at: $filePath")
    df
  }

  def rdd2df(rdd: RDD[Row], schema: StructType): DataFrame = {
    val df = spark.createDataFrame(rdd, schema)
    df
  }

  private def createVideoCameraItemsDetectedRdd(
      rowCount: Int = 1000000
  ): RDD[Row] = {
    val possibleItemNames =
      Seq("ItemName1", "ItemName2", "ItemName3", "ItemName4", "ItemName5")
    val rowData = (1 to rowCount).map { _ =>
      Row(
        10L + (this.random.nextLong() % (110L - 10L)),
        this.random.nextInt(),
        this.random.nextInt(),
        possibleItemNames(this.random.nextInt(possibleItemNames.length)),
        this.random.nextInt()
      )
    }
    val rdd = spark.sparkContext.parallelize(rowData)
    rdd
  }

  private def createGeolocationRdd(rowCount: Int = 1000): RDD[Row] = {
    val possibleLocations =
      Seq("StreetA", "StreetB", "StreetC", "StreetD", "StreetE")
    val rowData = (1 to rowCount).map { _ =>
      Row(
        10L + (this.random.nextLong() % (110L - 10L)),
        possibleLocations(this.random.nextInt(possibleLocations.length))
      )
    }
    val rdd = spark.sparkContext.parallelize(rowData)
    rdd
  }

  def importOrGenerateData(
      parquetFilePath: String,
      schema: StructType
  ): DataFrame = {
    try {
      this.dfFromParquet(parquetFilePath, schema)
    } catch {
      case _: AnalysisException =>
        println("Generating data...") // thrown if the file is not loaded
      case _: Throwable => println("Unexpected exception caught")
    }
    if (schema == Schema.videoCameraItemsDetected) {
      this.rdd2df(this.createVideoCameraItemsDetectedRdd(), schema)
    } else if (schema == Schema.geolocation) {
      this.rdd2df(this.createGeolocationRdd(), schema)
    } else {
      throw new Exception(s"Unsupported schema: $schema")
    }
  }
}
