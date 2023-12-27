import mill._, scalalib._

object Glomers extends ScalaModule {
  println(millSourcePath)
  def scalaVersion = "2.13.12"

  def ivyDeps = Agg(
    ivy"com.lihaoyi::upickle:3.1.3"
  )

  // dunno how to extend from "ScalaTest"
  // https://github.com/com-lihaoyi/mill/blob/0.11.6/scalalib/src/mill/scalalib/TestModule.scala#L214-L216
  object test extends ScalaTests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest-funsuite:3.2.17")

    def testFramework = "org.scalatest.tools.Framework"
  }
}
