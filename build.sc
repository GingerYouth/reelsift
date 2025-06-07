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
  def mainClass = Some("Main")

  def checkstyleVersion = "10.25.0"
}