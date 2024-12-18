package com.tou

import com.tou.global.Schema
import com.tou.producer.DataProducer
import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.{desc, udf}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import scala.util.Try

object App {
  private def getVideoCameraItemsDetectedByLocationRdd(
      spark: SparkSession,
      rddA: RDD[Row],
      rddB: RDD[Row]
  ): RDD[Row] = {
    val lookupMap: Map[Long, String] = rddB
      .map { row =>
        (row.getLong(0), row.getString(1))
      } // Extract Long and String from Row
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

      val geographicalLocation =
        broadcastLookupMap.value.get(geographical_location_oid).orNull
      Row(
        geographical_location_oid,
        video_camera_oid,
        detection_oid,
        item_name,
        timestamp_detected,
        geographicalLocation
      )
    }
    rddC
  }

  private def getTopItemsRdd(
      rddC: RDD[Row],
      topItemCount: Int = 3
  ): RDD[Row] = {
    val topItemsRDD: RDD[Row] = rddC
      .map { row =>
        val itemName = row.getString(3) // Assuming item_name is at index 3
        (itemName, 1) // Create a tuple (itemName, count)
      }
      .reduceByKey(_ + _) // Sum counts for each item_name
      .map { case (itemName, count) =>
        Row(itemName, count)
      } // Create Row objects
      .sortBy { case Row(_: String, count: Int) =>
        -count
      } // Sort by count in descending order
      .zipWithIndex() // Add an index to maintain order
      .filter { case (_, index) =>
        index < topItemCount
      } // Take top N items based on index
      .map { case (row, _) => row } // Remove the index to return only Row

    topItemsRDD
  }

  private def getVideoCameraItemsDetectedByLocationDf(
      dfA: DataFrame,
      dfB: DataFrame
  ): DataFrame = {
    val lookupMap: Map[Long, String] = dfB.rdd
      .map { row =>
        (row.getLong(0), row.getString(1))
      } // Extract Long and String from Row
      .collect() // Collect to an Array of tuples
      .toMap // Convert to Map
    //val lookupUDF = udf((oid: Long) => lookupMap.get(oid).orNull, StringType)
    val lookupUDF = udf((oid: Long) => lookupMap.get(oid).orNull)

    val dfC = dfA.withColumn(
      "geographical_location",
      lookupUDF(dfA("geographical_location_oid"))
    )
    dfC
  }

  private def getTopItemsDf(
      dfC: DataFrame,
      topItemCount: Int = 3
  ): DataFrame = {
    val topItems = dfC
      .groupBy("item_name") // Group by item_name
      .count() // Count occurrences
      .orderBy(desc("count")) // Order by count in descending order
      .limit(topItemCount)
    topItems
  }

  def main(args: Array[String]): Unit = {
    val logger = Logger.getLogger(getClass.getName)
    logger.setLevel(Level.WARN)

    val tempDir =
      sys.env.getOrElse("TOU_TEMP_DIR", "C:\\Users\\tommy\\sparkly-scala\\tmp")
    val artifactsDir = sys.env.getOrElse(
      "TOU_ARTIFACTS_DIR",
      "C:\\Users\\tommy\\sparkly-scala\\artifacts"
    )

    val topItemCount: Int = Try(args(0)).getOrElse("3").toInt
    val geolocationParquetInputPath: String =
      Try(args(1)).getOrElse(artifactsDir + "\\geolocation.parquet")
    val videoCameraItemsDetectedParquetInputPath: String = Try(args(2))
      .getOrElse(artifactsDir + "\\videoCameraItemsDetected.parquet")
    val videoCameraItemsDetectedByLocationParquetOutputPath: String = Try(
      args(3)
    ).getOrElse(artifactsDir + "\\videoCameraItemsDetectedByLocation.parquet")
    val topItemsParquetOutputPath: String =
      Try(args(4)).getOrElse(artifactsDir + "\\topItems.parquet")

    println(s"topItemCount: $topItemCount")
    println(s"geolocationParquetInputPath: $geolocationParquetInputPath")
    println(
      s"videoCameraItemsDetectedByLocationParquetOutputPath: $videoCameraItemsDetectedByLocationParquetOutputPath"
    )
    println(
      s"videoCameraItemsDetectedParquetInputPath: $videoCameraItemsDetectedParquetInputPath"
    )
    println(s"topItemsParquetOutputPath: $topItemsParquetOutputPath")

    val spark = SparkSession
      .builder()
      .appName("tou-app")
      .master("local[*]") // Use all available CPU cores
      .config("spark.local.dir", tempDir)
      .getOrCreate()

    spark.sql(
      "set spark.sql.legacy.allowUntypedScalaUDF=true"
    ) // Suppress error in docker container about the non-inferrable type in UDF

    val producer = new DataProducer(spark)

    val dfA = producer.importOrGenerateData(
      videoCameraItemsDetectedParquetInputPath,
      Schema.videoCameraItemsDetected
    )
    val dfB = producer.importOrGenerateData(
      geolocationParquetInputPath,
      Schema.geolocation
    )

    val rddA = dfA.rdd
    val rddB = dfB.rdd

    // Data transformations using RDD only
    val rddC = this.getVideoCameraItemsDetectedByLocationRdd(spark, rddA, rddB)
    val dfFromRddC =
      producer.rdd2df(rddC, Schema.videoCameraItemsDetectedByLocation)

    val topItemsRdd = this.getTopItemsRdd(rddC, topItemCount = topItemCount)
    val dfFromTopItemsRdd = producer.rdd2df(topItemsRdd, Schema.itemNameByCount)

    // Data transformations using DataFrame only
    val dfC = this.getVideoCameraItemsDetectedByLocationDf(dfA, dfB)
    val topItemsDf = this.getTopItemsDf(dfC, topItemCount = topItemCount)

    // Check if the results of RDD-based transformations and DF-based transformations are the same
    if (!dfFromRddC.except(dfC).isEmpty && dfC.except(dfFromRddC).isEmpty) {
      throw new Exception(
        "DataFrame check: results from RDD-transformations differ from DataFrame-transformations!"
      )
    }

    if (
      !dfFromTopItemsRdd.except(topItemsDf).isEmpty && topItemsDf
        .except(dfFromTopItemsRdd)
        .isEmpty
    ) {
      throw new Exception(
        "DataFrame check: results from RDD-transformations differ from DataFrame-transformations!"
      )
    }

    if (
      !rddC
        .subtract(topItemsDf.rdd)
        .isEmpty() && topItemsDf.rdd.subtract(rddC).isEmpty()
    ) {
      throw new Exception(
        "RDD check: results from RDD-transformations differ from DataFrame-transformations!"
      )
    }

    if (
      !topItemsRdd
        .subtract(topItemsDf.rdd)
        .isEmpty() && topItemsDf.rdd.subtract(topItemsRdd).isEmpty()
    ) {
      throw new Exception(
        "RDD check: results from RDD-transformations differ from DataFrame-transformations!"
      )
    }

    // Write to parquet files if freshly generated input data is needed
//    println("videoCameraItemsDetected:")
//    dfA.show()
//    producer.dfToParquet(dfA, artifactsDir + "/videoCameraItemsDetected_NEW.parquet")
//
//    println("geolocation:")
//    dfB.show()
//    producer.dfToParquet(dfB, artifactsDir + "/geolocation_NEW.parquet")

    // Display and save the transformed data
    println("videoCameraItemsDetectedByLocation:")
    dfC.show()
    producer.dfToParquet(
      dfC,
      videoCameraItemsDetectedByLocationParquetOutputPath
    )

    println(s"itemNameByCount (top $topItemCount)")
    topItemsDf.show()
    producer.dfToParquet(topItemsDf, topItemsParquetOutputPath)

    println("Data processing done.")
    spark.stop()
  }
}
