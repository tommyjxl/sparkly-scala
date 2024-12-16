import org.apache.spark.sql.SparkSession

object UtopiaApp {
  def main(args: Array[String]): Unit = {
    val temp_dir = sys.env.getOrElse("UTOPIA_TEMP_DIR", "C:\\Users\\tommy\\sparkly-scala\\tmp")
    val artifacts_dir = sys.env.getOrElse("UTOPIA_ARTIFACTS_DIR", "C:\\Users\\tommy\\sparkly-scala\\artifacts")
    val input_filepath = artifacts_dir + "/dummy.md"

    // Create a Spark session
    val spark = SparkSession.builder()
      .appName("Utopia App")
      .master("local[*]")
      .config("spark.local.dir", temp_dir)
      .getOrCreate()

    // Import implicits for encoders
    import spark.implicits._

    // Read the text file into a DataFrame
    val textFile = spark.read.textFile(input_filepath)

    // Split lines into words, flatten the result, and count occurrences
    val wordCounts = textFile
      .flatMap(line => line.split("\\W+")) // Split by non-word characters
      .groupByKey(word => word) // Group by each word
      .count() // Count occurrences

    // Show the results
    wordCounts.show()

    // Stop the Spark session
    spark.stop()
  }
}
