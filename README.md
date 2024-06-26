scala-optparse-applicative
==========================

[![scaladoc](https://javadoc.io/badge2/com.github.xuwei-k/optparse-applicative_2.13/javadoc.svg)](https://javadoc.io/doc/com.github.xuwei-k/optparse-applicative_2.13/latest/optparse_applicative/index.html)

A port of the [optparse-applicative][1] library to the Scala programming language.

Most functionality has been ported, except completion.

This library depends on [Scalaz][2] for functional data structures, type classes and combinators.

How to get it
-------------

for jvm

```scala
libraryDependencies += "com.github.xuwei-k" %% "optparse-applicative" % "0.9.4"
```


for scala-js, scala-native

```scala
libraryDependencies += "com.github.xuwei-k" %%% "optparse-applicative" % "0.9.4"
```


License
-------
This library is distributed under a [BSD 3-Clause][3] license (see `LICENSE`).

Simple example
--------------

This example follows the one from the [optparse-applicative][1] docs.

```scala
case class Sample(hello: String, quiet: Boolean)

object SampleMain {

  val sample: Parser[Sample] =
    ^(
      strOption(long("hello"), metavar("TARGET"), help("Target for the greeting")),
      switch(long("quiet"), help("Whether to be quiet"))
    )(Sample.apply)

  def greet(s: Sample): Unit = s match {
    case Sample(h, false) => println("Hello, " ++ h)
    case _ =>
  }

  def main(args: Array[String]): Unit = {
    val opts = info(sample <*> helper,
      progDesc("Print a greeting for TARGET"),
      header("hello - a test for scala-optparse-applicative"))
    greet(execParser(args, "SampleMain", opts))
  }

}
```

When run with the `--help` option, it prints:

    hello - a test for scala-optparse-applicative
    
    Usage: SampleMain --hello TARGET [--quiet]
      Print a greeting for TARGET
    
    Available options:
      -h,--help                Show this help text
      --hello TARGET           Target for the greeting
      --quiet                  Whether to be quiet


Scalaz 7.2.x
--------------

<https://github.com/xuwei-k/optparse-applicative/tree/0.8.x>

[1]: https://hackage.haskell.org/package/optparse-applicative
[2]: https://github.com/scalaz/scalaz
[3]: http://opensource.org/licenses/BSD-3-Clause
