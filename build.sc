import mill._, mill.javalib._, checkstyle._

object reel extends JavaModule with CheckstyleModule {
  def mainClass = Some("Main")

  def checkstyleVersion = "10.25.0"

}