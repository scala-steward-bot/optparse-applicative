package optparse_applicative.extra

import optparse_applicative.builder._
import optparse_applicative.common._
import optparse_applicative.helpdoc._
import optparse_applicative.internal._
import optparse_applicative.types._
import optparse_applicative.builder.internal.OptionFields

import scalaz.{-\/, \/-, ~>, Const}
import scalaz.syntax.applicative._
import scalaz.syntax.semigroup._
import scalaz.Endo.endoInstance

private[optparse_applicative] trait Extra {

  /** A hidden "helper" option which always fails */
  def helper[A]: Parser[A => A] =
    abortOption(ShowHelpText, long[OptionFields, A => A]("help") <> short('h') <> help("Show this help text") <> hidden)

  def execParser[A](args: Array[String], progName: String, pinfo: ParserInfo[A]): A =
    customExecParser(args.toList, progName, prefs(idm[PrefsMod]), pinfo)

  def customExecParser[A](args: List[String], progName: String, pprefs: ParserPrefs, pinfo: ParserInfo[A]): A =
    handleParseResult(progName, execParserPure(pprefs, pinfo, args))

  def handleParseResult[A](progName: String, result: ParserResult[A]): A =
    result match {
      case Success(a) => a
      case Failure(f) =>
        val (msg, exit) = renderFailure(f, progName)
        exit match {
          case ExitSuccess => println(msg)
          case _ => Console.err.println(msg)
        }
        sys.exit(exit.toInt)
    }

  def execParserPure[A](pprefs: ParserPrefs, pinfo: ParserInfo[A], args: List[String]): ParserResult[A] = {
    val p = runParserInfo[P, A](pinfo, args)
    runP(p, pprefs) match {
      case (_, \/-(r)) => Success(r)
      case (ctx, -\/(err)) => Failure(parserFailure(pprefs, pinfo, err, ctx))
    }
  }

  /** Generate a ParserFailure from a ParseError in a given Context. */
  def parserFailure[A](
    pprefs: ParserPrefs,
    pinfo: ParserInfo[A],
    msg: ParseError,
    ctx: Context
  ): ParserFailure[ParserHelp] =
    ParserFailure { progName =>
      val exitCode = msg match {
        case ErrorMsg(_) | UnknownError => ExitFailure(pinfo.failureCode)
        case _ => ExitSuccess
      }

      def withContext[AA, B](ctx: Context, pinfo: ParserInfo[AA], f: List[String] => ParserInfo ~> (Const[B, *])): B =
        ctx match {
          case NullContext => f(Nil)(pinfo).getConst
          case HasContext(n, i) => f(n)(i).getConst
        }

      def usage_help[AA](progName: String, names: List[String], i: ParserInfo[AA]): ParserHelp =
        msg match {
          case InfoMsg(_) => ParserHelp.empty
          case _ =>
            usageHelp(
              Chunk.vcatChunks(
                List(
                  parserUsage(pprefs, i.parser, unwords(progName :: names)).pure[Chunk],
                  i.progDesc.map(Doc.indent(2, _))
                )
              )
            )
        }

      val error_help: ParserHelp =
        errorHelp(msg match {
          case ShowHelpText => Chunk.empty
          case ErrorMsg(m) => Chunk.fromString(m)
          case InfoMsg(m) => Chunk.fromString(m)
          case UnknownError => Chunk.empty
        })

      val showFullHelp = msg match {
        case ShowHelpText => true
        case _ => pprefs.showHelpOnError
      }

      def baseHelp[AA](i: ParserInfo[AA]): ParserHelp =
        if (showFullHelp) headerHelp(i.header) |+| footerHelp(i.footer) |+| parserHelp(pprefs, i.parser)
        else ParserHelp.empty

      val h = withContext[A, ParserHelp](
        ctx,
        pinfo,
        names =>
          new (ParserInfo ~> (Const[ParserHelp, *])) {
            def apply[AA](fa: ParserInfo[AA]): Const[ParserHelp, AA] =
              Const {
                baseHelp(fa) |+| usage_help(progName, names, fa) |+| error_help
              }
          }
      )

      (h, exitCode, pprefs.columns)
    }

  def renderFailure(failure: ParserFailure[ParserHelp], progName: String): (String, ExitCode) = {
    val (h, exit, cols) = failure.run(progName)
    (ParserHelp.renderHelp(cols, h), exit)
  }
}
