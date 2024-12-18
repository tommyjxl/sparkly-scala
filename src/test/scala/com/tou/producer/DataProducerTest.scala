package com.tou.producer

import com.tou.global.Schema
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DataProducerTest
    extends AnyFunSuite
    with BeforeAndAfterAll
    with Matchers {
  private var spark: SparkSession = _
  private var dataProducer: DataProducer = _

  override def beforeAll(): Unit = {
    spark = SparkSession
      .builder()
      .appName("DataProducerTest")
      .master("local[*]")
      .getOrCreate()

    dataProducer = new DataProducer(spark, randomSeed = 42L)
  }

  override def afterAll(): Unit = {
    spark.stop()
  }

  test("importOrGenerateData should read from Parquet if file exists") {
    val df: DataFrame =
      dataProducer.importOrGenerateData(
        "artifacts/geolocation.parquet",
        Schema.geolocation
      )
    df.count() should be(1000)
  }

  test("importOrGenerateData should generate= data if the file is missing") {
    val generated_df: DataFrame =
      dataProducer.importOrGenerateData(
        "artifacts/geolocation2.parquet",
        Schema.geolocation
      )
    val df: DataFrame =
      dataProducer.importOrGenerateData(
        "artifacts/geolocation.parquet",
        Schema.geolocation
      )
    generated_df.count() should be(1000)

    // the generated data should be different
    val difference =
      generated_df.except(df).count() + df.except(generated_df).count()
    difference should be > 0L
  }

  test(
    "importOrGenerateData should throw an exception for unsupported schema"
  ) {
    val unsupportedSchema = StructType(
      Seq(
        StructField("unsupportedField", LongType)
      )
    )
    val parquetFilePath = "path/to/nonexistent/parquet/file"
    an[Exception] should be thrownBy {
      dataProducer.importOrGenerateData(parquetFilePath, unsupportedSchema)
    }
  }
}
