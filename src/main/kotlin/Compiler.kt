// driver for the Jack2 compiler
// calls the lexer and the parser
// Kittekat Oct/2017

package net.kittenberger.wolfgang.jack2c

import java.io.File

// singleton to allow access to file informations
object FIL {
  lateinit var lines: List<String>
  lateinit var fileName: String
  lateinit var baseName: String
  lateinit var basePath: String
  lateinit var clasName: String         // for symTable redefinition warning
  lateinit var subrName: String         // for symTable redefinition warning
}

// Utilities ========================================================

// create a directory if not yet existing
fun checkDir(dir: String) {
  val path = File(dir)
  if (!path.exists())
    makeDir(path)
  else {
    if (path.isFile())
      LOG.FATAL("The directory $dir could not be created" +
        " as there is a file with the same name")
  }
}

// create a directory
fun makeDir(path: File) {
  try {
    path.mkdir()
  }
  catch (t: Throwable) {
    LOG.FATAL("makeDir failed: ${t.message} $t")
  }
}

fun showErrorPos(row: Int, col: Int): ArrayList<String> {
  var txt: ArrayList<String> = arrayListOf()
  txt.add(FIL.lines[row-1])
  var blank = ""
  (1..col).forEach {blank = blank + " "}
  txt.add("${blank}^")
  return txt
}

// ==================================================================
class Compiler(val file: File) {

  fun compile() {
    val fileName = file.getName()
    val basePath = file.getAbsolutePath().substringBeforeLast("/")
    val baseName = fileName.substringBeforeLast(".")
    LOG.INFO("Compiling $fileName")
    LOG.DEBUG("basePath: $basePath")
    LOG.DEBUG("baseName: $baseName")

    val lines = getLines(file)
    FIL.fileName = fileName
    FIL.baseName = baseName
    FIL.basePath = basePath
    FIL.lines = lines

    // syntax analysis
    val lexer = Lexer(lines)
    val tokens = lexer.getTokens()
    LOG.INFO("  ${tokens.size} tokens found")
    if (OPT.LEXFILE)
      lexer.lexDump(tokens)

    // semantic anal<sis
    val parser = Parser(tokens)
    val ast = parser.parse()
    LOG.INFO("  ${AstTree.size} AST entries identified")
    if (OPT.ASTFILE)
      parser.astDump(ast)

    // VM code geneteration
    val generator = Generator()
    generator.doClass(ast)
    LOG.INFO("  ${VMWriter.size} VM records generated")

  }

  // prepare source data
  fun getLines(file: File): List<String> {
    val text = file.readText()
    val lines = text.split("\n")
    return lines
  }
}
