// Lexer for the Jack2 compiler
// extracts tokens from the source file
// kittekat Sep/2017

package net.kittenberger.wolfgang.jack2c

import java.io.File

// ==================================================================
class Lexer(val lines: List<String>) {

  // allow static access to the predefined sets
  companion object TOKSETS {
    val SYMBOLS = setOf("{", "}", "(", ")", "[", "]", ".", ",", ";", "+", "-",
                          "*", "/", "&", "|", "<", ">", "=", "~", "'")

    val KEYWORDS = setOf( "class", "constructor", "function", "method",
                "field", "static", "var", "int", "char", "boolean", "break",
                "void", "let", "do", "if", "else", "while", "return", "const")

    val KEYWORDCONSTANTS = setOf("true", "false", "null", "this")
  }

  var line = ""            // current source line
  var tRow = 0             // position of token in current line

  // defined are SYMBOLS, KEYWORDS, KEYWORDCONSTANTS and the comment syntax
  fun getTokens(): ArrayList< Token > {
    val tokens: ArrayList< Token > = arrayListOf()   // tokens of this file
    // match separators: whitespaces, doublequote and all (escaped) symbols
    val regex = Regex("""((\s+)|["\{\}\(\)\[\]\.,;\+\-\*/&\|<>=~'])""")

    try {     // getNextLine will run into bounds check
      while (true) {
        val (lin, row) = getNextLine(tRow)
        line = lin
        tRow = row
        var index = 0
        val limit = line.length

        // loop searching the separators
        while (index < limit) {
          val matchRes = regex.find(line, index)
          if (matchRes != null) {
            val value = matchRes.value
            val first = matchRes.range.first
            if (index < first) {
              val nonSep = nonSeparator(line.substring(index..first - 1), index)
              tokens.add(nonSep)
            }
            val (tok, adv) = separator(value, first)
            //val (typ, _, _) = tok
            if (tok.typ != TOKTYP.IGNORED) {
              tokens.add(tok)
            }
            index = adv
          }
          else
            index = limit     // no separator found: done
        }
      }
    }
    catch (e: IndexOutOfBoundsException) {
    }
    LOG.INFO("  $tRow source lines")
    return tokens
  }

  // generate the lex/token.xml file
  fun lexDump(tokens: ArrayList<Token>) {
    val path = FIL.basePath + "/lex"
    checkDir(path)
    val lexFile = path + "/" + FIL.baseName + ".xml"
    LOG.DEBUG("lexFile: $lexFile")
    val lexf = File(lexFile)
    val txt = arrayListOf("tokens")
    tokens.forEach {
      //val (tok, str, _) = it
      val typ = it.typ
      val esc = when (it.str) {
        ">" -> "&gt"
        "<" -> "&lt"
        "&" -> "&amp"
        else -> it.str
      }
      txt.add("<$typ> $esc </$typ>")
    }
    txt.add("</tokens>")
    lexf.writeText(txt.joinToString("\n"))
  }

// ==================================================================
// special handling for blanks, doublequote, quote and comments
  private fun separator(value: String, beg: Int): Pair<Token, Int> {
    when {
      isBlank(value)  -> return doBlank(value, beg)
      value == "\""   -> return doString(beg)
      value == "'"    -> return doQuote(beg)
      value == "/"    -> return doSlash(beg)
      //else -> return Pair(formToken(TOKTYP.SYMBOL, value, tRow, beg), beg + 1)
      else -> return Pair(Token(TOKTYP.SYMBOL, value, tRow, beg), beg + 1)
    }
  }

  // there is a non_separator token in front of the separator
  // maybe keyword(constant), integer, identifier
  fun nonSeparator(value: String, beg: Int): Token {
    when {
      isKeyword(value)          -> return doKeyword(value, beg)
      isKeywordConstant(value)  -> return doKeywordConstant(value, beg)
      isInteger(value)          -> return doInteger(value, beg)
      isIdentifier(value)       -> return doIdentifier(value, beg)
      else -> return Token(TOKTYP.UNKNOWN, value, tRow, beg)
    }
  }

  private fun isBlank(value: String): Boolean {
    return Regex("""\A\s+\z""").matches(value)
  }

  private fun isKeyword(value: String): Boolean {
    return (KEYWORDS.contains(value))
  }

  private fun isKeywordConstant(value: String): Boolean {
    return (KEYWORDCONSTANTS.contains(value))
  }

  private fun isInteger(value: String): Boolean {
    return Regex("""\A\d+\z""").matches(value)
  }

  private fun isIdentifier(value: String): Boolean {
    // sequence of any letter, digit and underscore, not starting with a digit
    return Regex("""\A([A-Za-z]|_)([A-Za-z0-9]|_)*\z""").matches(value)
  }

  private fun doBlank(value: String, beg: Int): Pair<Token, Int> {
    val tok = Token(TOKTYP.IGNORED, "", tRow, beg)
    val adv = beg + value.length
    return Pair(tok, adv)
  }

  private fun doString(beg: Int): Pair<Token, Int> {
    var nxt = beg
    do {                        // find end of string
      nxt = line.indexOf('"', nxt + 1)
      if (nxt == -1) {
        LOG.ERROR("(file: ${FIL.fileName}, line: $tRow, col: ${beg + 1}" +
        "/ Missing string termination")
        val tok = Token(TOKTYP.UNKNOWN,
          line.substring(beg..line.length-1), tRow, beg)
        val adv = line.length       // stop scanning this line
        return Pair(tok, adv)
      }
    } while (line[nxt - 1] == '\\')    // ignore escaped doublequote
    val tok = Token(TOKTYP.STRING, line.substring((beg+1)..(nxt-1)), tRow, beg)
    val adv = nxt + 1
    return Pair(tok, adv)
  }

  private fun doQuote(beg: Int): Pair<Token, Int> {
    var nxt = beg + 1
    var char = Character.toString(line[nxt])
    nxt++
    if (char == "\\") {           // escaped char
      char = Character.toString(line[nxt])
      nxt++
    }
    if (Character.toString(line[nxt]) == "'") {
      val tok = Token(TOKTYP.CHARCONSTANT, char, tRow, beg)
      val adv = nxt + 1
      return Pair(tok, adv)
    }
    else {
      LOG.ERROR("(file: ${FIL.fileName}, line: $tRow, col: ${beg + 1}" +
      "/ Unterminated CharConstant")
      val tok = Token(TOKTYP.UNKNOWN, char, tRow, beg)
      val adv = nxt + 1
      return Pair(tok, adv)
    }
  }

  private fun doSlash(beg: Int): Pair<Token, Int> {
    val char = line[beg + 1]
    when {
      char == '/' -> return doShortComment(beg)
      char == '*' -> return doLongComment(beg)
      else -> return Pair(Token(TOKTYP.SYMBOL, "/", tRow, beg), beg + 1)
    }
  }

  private fun doShortComment(beg: Int): Pair<Token, Int> {
    return Pair(Token(TOKTYP.IGNORED, "//", tRow, beg), line.length)
  }

  private fun doLongComment(beg: Int): Pair<Token, Int> {
    var nxt = beg + 2
    nxt = line.indexOf("*/", nxt)           // find termination
    while (nxt == -1) {                     // nothing found
      val (lin, row) = getNextLine(tRow)    // try in nect line
      line = lin
      tRow = row
      nxt = line.indexOf("*/")
    }
    return Pair(Token(TOKTYP.IGNORED, "/*", tRow, beg), nxt + 2)
  }

  private fun doKeyword(value: String, beg: Int): Token {
    return Token(TOKTYP.KEYWORD, value, tRow, beg)
  }

  private fun doKeywordConstant(value: String, beg: Int): Token {
    return Token(TOKTYP.KEYWORDCONSTANT, value, tRow, beg)
  }

  private fun doInteger(value: String, beg: Int): Token {
    return Token(TOKTYP.INTEGER, value, tRow, beg)
  }

  private fun doIdentifier(value: String, beg: Int): Token {
    return Token(TOKTYP.IDENTIFIER, value, tRow, beg)
  }

  // get NextLine will be called from two locations (longComment handling)
  private fun getNextLine(row: Int): Pair<String, Int> {
    // add a blank to have a separator at the end of the line
    val ln = lines[row] + " "
    return Pair(ln, row+1)
  }
}
