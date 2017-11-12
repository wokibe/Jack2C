// an enhanced implementation of the Jack compiler
// see http/nand2tetris.org

// as this is my first attempt to write a compiler, I had no idea how to start
// so Google delivered some ideas:
//  https://github.com/SeaRbSg/nand2tetris/tree/master/phiggins/10 & /11
// kittekat, Sep 2017

package net.kittenberger.wolfgang.jack2c

import java.io.File
import com.xenomachina.argparser.*
//import net.kittenberger.wolfgang.jack2c.Compiler
import net.kittenberger.wolfgang.mylogger.*

const val JACK2C_VERSION = "0.9.2"

// get the CLI arguments
class ParsedArgs(parser: ArgParser) {
  val symb by parser
    .flagging("-s", "--symbols",  help = "generate symbol tables")
  val jack by parser
    .flagging("-i", "--include",  help = "include jack statements in .vm file")
  val enhc by parser
    .flagging("-e", "--enhance",  help = "enhance comments in .vm file")
  val lexf by parser
    .flagging("-l", "--lexfile",  help = "generate a token.xml file")
  val astf by parser
    .flagging("-a", "--astfile",  help = "generate an ast.xml file")
  val levl by parser
    .mapping(
      "--debug" to Level.DEBUG,
      "--info"  to Level.INFO,
      "--warn"  to Level.WARN,
      "--error" to Level.ERROR,
      "--fatal" to Level.FATAL,
      help = "select logging Level"
      )
    .default(Level.FATAL)
  val version by parser
    .flagging("-v", "--version", help = "show Jack2c version")
  val path by parser
    .positional("PATH", help = "source & destination folder")
    .default("")
}

// a Singleton to allow all classes access to options
object OPT {
  var SYMB: Boolean = false
  var JACK: Boolean = false
  var ENHC: Boolean = false
  var LEXFILE: Boolean = false
  var ASTFILE: Boolean = false
}

// a Singleton to allow all classes access to logger
object LOG {
  lateinit var Log: Logger
  fun DEBUG(txt: String) = Log.debug(txt)
  fun INFO(txt: String)  = Log.info(txt)
  fun WARN(txt: String)  = Log.warn(txt)
  fun ERROR(txt: String) = Log.error(txt)
  fun FATAL(txt: String) = Log.fatal(txt)
}

// check CLI arguments and distribute
fun main(args: Array<String>) = mainBody("Jack2c") {
  ParsedArgs(ArgParser(args,
                       ArgParser.Mode.GNU,
                       DefaultHelpFormatter(
                          prologue = " Compile *.jack files in PATH. " +
                            "Generate *.vm files in PATH/vm."
                          )
                      )).run {

    if (version) {
      println("Jack2c compiler - version $JACK2C_VERSION")
      System.exit(0)
    }

    // prepare the global singletons with logger and options
    LOG.Log = Logger(levl)
    OPT.SYMB = symb
    OPT.JACK = jack
    OPT.ENHC = enhc
    OPT.LEXFILE = lexf
    OPT.ASTFILE = astf

    val folder = File(path)
    LOG.DEBUG("path: $path")
    if (!folder.exists())
      LOG.FATAL("The path $path does not exist (use --help)")

    if (!folder.isDirectory())
      compileFile(folder)
    else {
      // loop over the directory‚
      val files = folder.listFiles()
      files.forEach {
        compileFile(it)
      }
    }
  }
}

// prepare for compiler
fun compileFile(file: File) {
  if (file.isFile) {
    val name = file.getName()
    // LOG.DEBUG("Name: $name")
    if (name.contains('.')) {
      val ext = file.extension
      if (ext == "jack") {
        val compiler = Compiler(file)
        compiler.compile()
      }
    }
  }
}
