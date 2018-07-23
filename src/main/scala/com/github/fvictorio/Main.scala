package com.github.fvictorio

import breeze.linalg.DenseVector
import info.debatty.java.graphs.{Node, SimilarityInterface}
import info.debatty.spark.knngraphs.builder.NNDescent
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.sql.{SparkSession, functions => f}

object Main {
  def main(args: Array[String]) {
    val spark = SparkSession.builder.appName("EMNIST")
      .master("local[*]")
      .getOrCreate()
    val sc = spark.sparkContext
    sc.setLogLevel("ERROR")
    import spark.implicits._

    val dfRaw = spark.read.format("csv")
      .option("header", false)
      .load(
        if (args.length > 0)
          args(0)
        else
          this.getClass.getResource("/emnist-head.csv").getPath
      )

    var df = dfRaw
        .map(row => {
          val label = row.getString(0).toLong
          val head = row.getString(1).toDouble
          val tail = (2 to 784).map(i => row.getString(i).toDouble)
          val features = Vectors.dense(head, tail: _*)

          (label, features)
        })
        .withColumn("id", f.monotonically_increasing_id())
        .toDF("correctLabel", "features", "id")
        .select("id", "features", "correctLabel")

    val nodes = df.select("id", "features")
      .rdd
      .map(row => (row.getLong(0), row.getAs[Vector](1)))
      .map {
        case (id, features) => new Node(id.toString, features)
      }

    val nnd = new NNDescent[Vector]

    nnd.setSimilarity(new SimilarityInterface[Vector] {
      override def similarity(a: Vector, b: Vector): Double = {
        val difference = subtract(a, b)
        1.0 / (1.0 + Vectors.norm(difference, 2.0))
      }
    })

    nnd.setK(10)

    println("Compute graph")
    val graph = nnd.computeGraph(nodes)

    val count = graph.count
    println(s"Count: $count")
  }

  def subtract(v1: Vector, v2: Vector): Vector = {
    val bv1 = new DenseVector(v1.toArray)
    val bv2 = new DenseVector(v2.toArray)

    Vectors.dense((bv1 - bv2).toArray)
  }
}
