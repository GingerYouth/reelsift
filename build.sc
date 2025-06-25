import mill._, mill.javalib._, checkstyle._
import mill.{Module, T, _}

def sources = Task.Source {
  "reel/src"
}
def resources = Task.Source {
  "resources"
}

def allSources = Task {
  os.walk(sources().path)
    .filter(_.ext == "java")
    .map(PathRef(_))
}

def lineCount: T[Int] = Task {
  println("Computing line count")
  allSources()
    .map(p => os.read.lines(p.path).size)
    .sum
}

object reel extends JavaModule with CheckstyleModule {
  def mainClass = Some("reel.src.main.java.sift.Main")
  def checkstyleVersion = "10.25.0"

  def ivyDeps = Agg(
    ivy"org.jsoup:jsoup:1.20.1",
    ivy"org.json:json:20250517",
    ivy"org.telegram:telegrambots-longpolling:8.3.0",
    ivy"org.telegram:telegrambots-client:8.3.0"
  )
}