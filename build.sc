import mill._, scalalib._

object Glomers extends ScalaModule {
  println(millSourcePath)
  def scalaVersion = "2.13.12"

  def ivyDeps = Agg(
    ivy"com.lihaoyi::upickle:3.1.3"
  )
}
