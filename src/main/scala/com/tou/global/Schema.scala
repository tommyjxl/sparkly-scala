package com.tou.global

import org.apache.spark.sql.types._

object Schema {
  val videoCameraItemsDetected: StructType = StructType(
    List(
      StructField("geographical_location_oid", LongType, nullable = false),
      StructField("video_camera_oid", IntegerType, nullable = false),
      StructField("detection_oid", IntegerType, nullable = false),
      StructField("item_name", StringType, nullable = false),
      StructField("timestamp_detected", IntegerType, nullable = false)
    )
  )

  val geolocation: StructType = StructType(
    List(
      StructField("geographical_location_oid", LongType, nullable = false),
      StructField("geographical_location", StringType, nullable = false)
    )
  )

  val videoCameraItemsDetectedByLocation: StructType =
    videoCameraItemsDetected.add(
      StructField("geographical_location", StringType, nullable = true)
    )

  val itemNameByCount: StructType = StructType(
    List(
      StructField("item_name", StringType, nullable = false),
      StructField("count", IntegerType, nullable = false)
    )
  )
}
