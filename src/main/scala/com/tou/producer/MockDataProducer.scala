package com.tou.producer

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

import scala.util.Random

class MockDataProducer(spark: SparkSession) {
  def createVideoCameraItemsDetectedRdd(rowCount: Int = 1000000): RDD[Row] = {
    val random = new Random()
    val possibleItemNames = Seq("ItemName1", "ItemName2", "ItemName3", "ItemName4", "ItemName5")
    val rowData = (1 to rowCount).map { _ =>
      Row(
        10L + (random.nextLong() % (110L - 10L)),
        random.nextInt(),
        random.nextInt(),
        possibleItemNames(random.nextInt(possibleItemNames.length)),
        random.nextInt()
      )
    }
    val rdd = spark.sparkContext.parallelize(rowData)
    rdd
  }

  def createGeolocationRdd(rowCount: Int = 1000): RDD[Row] = {
    val random = new Random()
    val possibleLocations = Seq("StreetA", "StreetB", "StreetC", "StreetD", "StreetE")
    val rowData = (1 to rowCount).map { _ =>
      Row(
        10L + (random.nextLong() % (110L - 10L)),
        possibleLocations(random.nextInt(possibleLocations.length))
      )
    }
    val rdd = spark.sparkContext.parallelize(rowData)
    rdd
  }

  def rdd2df(rdd: RDD[Row], schema: StructType): DataFrame = {
    val df = spark.createDataFrame(rdd, schema)
    df
  }

  def dfToParquet(df: DataFrame, filePath: String): Unit = {
    df.write.mode("overwrite").parquet(filePath)
    println(s"DataFrame written to Parquet file at: $filePath")
  }

  def dfFromParquet(filePath: String): DataFrame = {
    val df = spark.read.parquet(filePath)
    println(s"DataFrame read from Parquet file at: $filePath")
    df
  }
}