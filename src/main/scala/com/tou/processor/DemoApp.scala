package com.tou.processor

import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession, DataFrame}
import com.tou.global.Schema
import com.tou.producer.MockDataProducer
import org.apache.spark.sql.functions.{desc, udf}
import org.apache.spark.sql.types.StringType

object DemoApp {
  private def getVideoCameraItemsDetectedByLocationRdd(spark: SparkSession, rddA: RDD[Row], rddB: RDD[Row]): RDD[Row] = {
    val lookupMap: Map[Long, String] = rddB
      .map { row => (row.getLong(0), row.getString(1)) } // Extract Long and String from Row
      .collect() // Collect to an Array of tuples
      .toMap // Convert to Map

    // Broadcast the lookup map for efficient access
    val broadcastLookupMap = spark.sparkContext.broadcast(lookupMap)

    val rddC: RDD[Row] = rddA.map { row =>
      // Extract fields from Row using appropriate get methods
      val geographical_location_oid = row.getLong(0)
      val video_camera_oid = row.getInt(1)
      val detection_oid = row.getInt(2)
      val item_name = row.getString(3)
      val timestamp_detected = row.getInt(4)

      val geographicalLocation = broadcastLookupMap.value.get(geographical_location_oid).orNull
      Row(geographical_location_oid, video_camera_oid, detection_oid, item_name, timestamp_detected, geographicalLocation)
    }
    rddC
  }

  private def getTopItemsRdd(rddC: RDD[Row], topItemCount: Int = 10): RDD[Row] = {
    val topItemsRDD: RDD[Row] = rddC
      .map { row =>
        val itemName = row.getString(3) // Assuming item_name is at index 3
        (itemName, 1) // Create a tuple (itemName, count)
      }
      .reduceByKey(_ + _) // Sum counts for each item_name
      .map { case (itemName, count) => Row(itemName, count) } // Create Row objects
      .sortBy { case Row(itemName: String, count: Int) => -count } // Sort by count in descending order
      .zipWithIndex() // Add an index to maintain order
      .filter { case (_, index) => index < topItemCount } // Take top N items based on index
      .map { case (row, _) => row } // Remove the index to return only Row

    topItemsRDD
  }

  private def getVideoCameraItemsDetectedByLocationDf(dfA: DataFrame, dfB: DataFrame): DataFrame = {
    val lookupMap: Map[Long, String] = dfB.rdd
      .map { row => (row.getLong(0), row.getString(1)) } // Extract Long and String from Row
      .collect() // Collect to an Array of tuples
      .toMap // Convert to Map
    val lookupUDF = udf((oid: Long) => lookupMap.get(oid).orNull, StringType)

    val dfC = dfA.withColumn("geographical_location", lookupUDF(dfA("geographical_location_oid")))
    dfC
  }

  private def getTopItemsDf(dfC: DataFrame, topItemCount: Int = 10): DataFrame = {
    val topItems = dfC
      .groupBy("item_name") // Group by item_name
      .count() // Count occurrences
      .orderBy(desc("count")) // Order by count in descending order
      .limit(10)
    topItems
  }

  def main(args: Array[String]): Unit = {
    //val videoCameraItemsDetectedParquetInputPath = args.lift(0).getOrElse()
    val topItemCount = args.lift(0).getOrElse("10").toInt

    val logger = Logger.getLogger(getClass.getName)
    logger.setLevel(Level.WARN)

    val temp_dir = sys.env.getOrElse("TOU_TEMP_DIR", "C:\\Users\\tommy\\sparkly-scala\\tmp")
    val artifacts_dir = sys.env.getOrElse("TOU_ARTIFACTS_DIR", "C:\\Users\\tommy\\sparkly-scala\\artifacts")

    val spark = SparkSession.builder()
      .appName("tou-app")
      .master("local[*]") // Use all available CPU cores
      .config("spark.local.dir", temp_dir)
      .getOrCreate()

    spark.sql("set spark.sql.legacy.allowUntypedScalaUDF=true") // Suppress error in docker container about the uninferrable type in UDF

    val producer = new MockDataProducer(spark)
    val rddA = producer.createVideoCameraItemsDetectedRdd()
    val rddB = producer.createGeolocationRdd()

    val rddC = this.getVideoCameraItemsDetectedByLocationRdd(spark, rddA, rddB)
    val dfFromRddC = producer.rdd2df(rddC, Schema.videoCameraItemsDetectedByLocation)
    dfFromRddC.show()

    val topItemsRdd = this.getTopItemsRdd(rddC, topItemCount=topItemCount)
    val dfFromTopItemsRdd = producer.rdd2df(topItemsRdd, Schema.itemNameByCount)

    println("Top item names by count:")
    dfFromTopItemsRdd.show()

    val dfA = producer.rdd2df(rddA, Schema.videoCameraItemsDetected)
    val dfB = producer.rdd2df(rddB, Schema.geolocation)

    val dfC = this.getVideoCameraItemsDetectedByLocationDf(dfA, dfB)
    dfC.show()

    val topItemsDf = this.getTopItemsDf(dfC, topItemCount=topItemCount)
    topItemsDf.show()

    // Check if the results of RDD-based transformations and DF-based transformations are the same
    if (!rddC.subtract(dfC.rdd).isEmpty() && dfC.rdd.subtract(rddC).isEmpty()) {
      println("rddC: rdd and dataframe transformations produced different results!")
      throw new Exception("rddC: rdd and dataframe transformations produced different results!")
    }

    if (!topItemsRdd.subtract(topItemsDf.rdd).isEmpty() && topItemsDf.rdd.subtract(topItemsRdd).isEmpty()) {
      println("topItemsRdd: rdd and dataframe transformations produced different results!")
      throw new Exception("topItemsRdd: rdd and dataframe transformations produced different results!")
    }

    producer.dfToParquet(dfA, artifacts_dir + "\\videoCameraItemsDetected.parquet")
    producer.dfToParquet(dfB, artifacts_dir + "\\geolocation.parquet")
    producer.dfToParquet(dfC, artifacts_dir + "\\videoCameraItemsDetectedByLocation.parquet")
    producer.dfToParquet(topItemsDf, artifacts_dir + "\\topItems.parquet")

    println("Data processing done.")

    spark.stop()
  }
}
