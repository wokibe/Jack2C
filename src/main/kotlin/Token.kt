// instrumented access to tokens for the Jack2 compiler
// kittekat Oct/2017

package net.kittenberger.wolfgang.jack2c

enum class TOKTYP {
  SYMBOL, INTEGER, STRING, KEYWORD, KEYWORDCONSTANT,
  IDENTIFIER, CHARCONSTANT, UNKNOWN, IGNORED, BLANK, COMMENT
}

data class Token(val typ: TOKTYP,
                 val str: String,
                 val row: Int,
                 val col: Int)

// ==================================================================
class TokenStream(val tokens: ArrayList<Token>) {
  var pTok: Int = 0
  val tokLim: Int = tokens.size                    // highwater mark
  val TOKENS: HashMap<String, TOKTYP> = hashMapOf()   // static strings
  val tooFar = "Over the edge"
  val empty = Token(TOKTYP.UNKNOWN, tooFar, 0, 0)

/*
as the tokens are hold in an unmutable ArrayList
there is a pointer (pTOK) to the next available token
to look further in the future of the funnel use lookahead as offset

API:
  peek(lookahead=0)
      return a token from the funnel without consuming it

  tstStrs(strings, lookahead=0)
      return true if any of the token strings is in the specified funnel

  tstTyps(types, lookahead=0)
      return true if any of the token types is in the specified funnel

  eatStrs(strs)
      consume the next token if it contains any of the strings
      and return the string of this token
      otherwise raise a syntax error

  eatTyps(types)
      consume the next token if it contains any of the typs
      and return the string of this token
      otherwise raise a syntax error

  tstStr, tstTyp, eatStr and eatTyp are shorthands for one element
      (avoid the writing of arrayOf() in the parameter list)

  lastPos
      return the (row, col) Pair of the just consumed token
*/

  init {
    Lexer.SYMBOLS.forEach { TOKENS[it] = TOKTYP.SYMBOL }
    Lexer.KEYWORDS.forEach { TOKENS[it] = TOKTYP.KEYWORD }
    Lexer.KEYWORDCONSTANTS.forEach { TOKENS[it] = TOKTYP.KEYWORDCONSTANT }

    LOG.DEBUG("TokenStream.init/ baseName: ${FIL.baseName}")
  }

  fun peek(lookahead: Int = 0): Token {
    val nxt = pTok + lookahead
    if (nxt < tokLim)
      return tokens[nxt]
    else
      return empty
  }

  fun tstStrs(strings: Array<String>, lookahead: Int = 0): Boolean {
    val nxt = pTok + lookahead
    if (nxt < tokLim) {
      // val (toktyp, _, _) = tokens[nxt]
      val (_, str, _) = tokens[nxt]
      strings.forEach {
        // must be a reserved string
        if ((str == it) and (TOKENS.containsKey(it))) return true
      }
    }
    return false
  }

  fun tstTyps(types: Array<TOKTYP>, lookahead: Int = 0): Boolean {
    val nxt = pTok + lookahead
    if (nxt < tokLim) {
      val (toktyp, _, _) = tokens[nxt]
      types.forEach {
        if (toktyp == it) return true
      }
    }
    return false
  }

  fun eatStrs(strings: Array<String>): String {
    if (pTok < tokLim)
      if (tstStrs(strings)) {
        val token = peek()
        pTok++
        return token.str
      }
    raiseErrStr(strings)
    return tooFar
  }

  fun eatTyps(types: Array<TOKTYP>): String {
    if (pTok < tokLim)
      if (tstTyps(types)) {
        val token = peek()
        pTok++
        return token.str
      }
    raiseErrTyp(types)
    return tooFar
  }

  fun tstStr(string: String, lookahead: Int = 0): Boolean =
              tstStrs(arrayOf(string), lookahead)

  fun tstTyp(type: TOKTYP, lookahead: Int = 0): Boolean =
              tstTyps(arrayOf(type), lookahead)

  fun eatStr(string: String): String =
              eatStrs(arrayOf(string))

  fun eatTyp(type: TOKTYP): String =
              eatTyps(arrayOf(type))

  fun lastPos(): Pair<Int, Int> {
    val tok = tokens[pTok - 1]
    return Pair(tok.row, tok.col)
  }

// ==================================================================
  private fun raiseErrStr(strings: Array<String>) {
    val expected = strings.joinToString(" | ")
    return syntaxError(expected)
  }

  private fun raiseErrTyp(types: Array<TOKTYP>) {
    val expected = types.joinToString(" | ")
    return syntaxError(expected)
  }

  private fun syntaxError(expected: String) {
    val tok = peek()
    val typ = tok.typ
    val str = tok.str
    val row = tok.row
    val col = tok.col
    val txt: ArrayList< String > = arrayListOf()
    txt.add(
      "Syntax error in file ${FIL.fileName} " +
      "[$row/${col+1}]")
    val showPos = showErrorPos(row, col)
    showPos.forEach { txt.add(it) }
    txt.add("expected: $expected found: $typ $str")
    LOG.FATAL(txt.joinToString("\n"))
  }
}
