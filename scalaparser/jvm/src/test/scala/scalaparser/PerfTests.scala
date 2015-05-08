package scalaparser

import utest._

import scala.tools.nsc.{Global, Settings}

object PerfTests extends TestSuite{
  val tests = TestSuite{
    'GenJSCode{
      var current = Thread.currentThread().getContextClassLoader
      val files = collection.mutable.Buffer.empty[java.io.File]
      files.appendAll(
        System.getProperty("sun.boot.class.path")
          .split(":")
          .map(new java.io.File(_))
      )
      while(current != null){
        current match{
          case t: java.net.URLClassLoader =>
            files.appendAll(t.getURLs.map(u => new java.io.File(u.toURI)))
          case _ =>
        }
        current = current.getParent
      }

      val settings = new Settings()
      settings.usejavacp.value = true
      settings.classpath.append(files.mkString(":"))
      val global = new Global(settings)
      val run = new global.Run()

      // Last measurements, runs in 30s:
      // parboiled2: 446 443 447
      //
      // Initial parsing: 104 123 122
      // Parboiled2 is 3.9 times faster
      //
      // Current parsing: 373 371 422
      val input = scala.io.Source.fromFile(
        "target/repos/scala-js/compiler/src/main/scala/org/scalajs/core/compiler/GenJSCode.scala"
      ).mkString
      println("Optimizing Parser")

      val parser = Scala.CompilationUnit
      println("Loaded " + input.length + " bytes of input. Parsing...")

      (
        time(() => parser.parse(input, trace = false)),
        time(() => global.newUnitParser(input).parse())
      )
    }
  }

  def time(f: () => Unit) = {
    val start = System.currentTimeMillis()
    var count = 0
    while(System.currentTimeMillis() - start < 30000){
      f()
      count += 1
    }
    count
  }
}
