// label helper for Jack2c
// kittekat Oct/2017

package net.kittenberger.wolfgang.jack2c

// ==================================================================
class Labels {
  private var ifStack       = Stack<Pair<String,String>>()
  private var whileStack    = Stack<Pair<String,String>>()
  private var nextIf        = 0
  private var nextWhile     = 0
  private val ifTemplate    = Pair("I-ELS", "I-END")
  private val whileTemplate = Pair("W-EXP", "W-END")

  //  flow-control background
  //
  //  Jack code:                        VM code:
  //  if (cond) {                       cond-expression
  //                                    not
  //                                    if-goto I-ELSxx
  //    s1                              s1-statements
  //  }                                 goto I_ENDxx
  //  else {                            label I-ELSxx
  //    s2                              s2-statements
  //  }                                 label I-ENDxx
  //  ...                                  ...
  //
  //  while (cond) {                    label W-EXPxx
  //                                    cond-expression
  //                                    not
  //                                    if-goto W-ENDxx
  //    s1                              s1-statements
  //  }                                 goto W-EXPxx
  //                                    label W-ENDxx
  //   ...                              ...
  //
  // evaluation of cond-expression:
  //    0                 is interpreted as 'false'
  //    everthing else    is interpreted as 'true'
  //
  // if-goto behavior:
  //    if-goto label     jumps to label, if top of stack is not zero
  //
  //    so we dont need to test for 'true' (0xFF)
  //    but for ~'true' (=x00) to get the desired simple jump pattern
  //    that means the result of condition must be boolean
  //
  // implementation:
  //  this class maintains an IF and a WHILE stack which holds the labels
  //  and an increasing counters for IF and WHILE
  //
  // API for this class:
  //
  //                 must be initialized at the beginning of each subroutine
  //  pushIf()       generate the next pair of I-ELSxx/I-ENDxx on IfStack
  //  getIfEls()     get current I-ELSxx string
  //  getIfEnd()     get current I-ENDxx string
  //  popIf()        remove top of IfStack
  //  pushWhile()    generate the next pair of W-EXPxx/W-ENDxx on WhileStack
  //  getWhileExp()  get current W-EXPxx string
  //  getWhileEnd()  get current W-ENDxx string
  //  popWhile()     remove top of WhileStack
  //  isWhileEmpty() check if a break statement is allowed

  fun pushIf() {
    val labels = Pair( ifTemplate.first + "$nextIf",
                       ifTemplate.second + "$nextIf")
    ifStack.push(labels)
    nextIf++
  }

  fun getIfEls(): String {
    var result = ""
    try {
      result = ifStack.peek()!!.first
    }
    catch (e: KotlinNullPointerException) {
      LOG.FATAL("Labels: Can't peek() from empty ifStack")
    }
    return result
  }

  fun getIfEnd(): String {
    var result = ""
    try {
      result = ifStack.peek()!!.second
    }
    catch (e: KotlinNullPointerException) {
      LOG.FATAL("Labels: Can't peek() from empty ifStack")
    }
    return result
  }

  fun popIf() {
    try {
      ifStack.pop()!!
    }
    catch (e: KotlinNullPointerException) {
      LOG.FATAL("Labels: Can't pop() from empty ifStack")
    }
  }

  fun pushWhile() {
    val labels = Pair( whileTemplate.first + "$nextWhile",
                       whileTemplate.second + "$nextWhile")
    whileStack.push(labels)
    nextWhile++
  }

  fun getWhileExp(): String {
    var result = ""
    try {
      result = whileStack.peek()!!.first
    }
    catch (e: KotlinNullPointerException) {
      LOG.FATAL("Labels: Can't peek() from empty WhileStack")
    }
    return result
  }

  fun getWhileEnd(): String {
    var result = ""
    try {
      result = whileStack.peek()!!.second
    }
    catch (e: KotlinNullPointerException) {
      LOG.FATAL("Labels: Can't peek() from empty whileStack")
    }
    return result
  }

  fun popWhile() {
    try {
      whileStack.pop()!!
    }
    catch (e: KotlinNullPointerException) {
      LOG.FATAL("Labels: Can't pop() from empty whileStack")
    }
  }

  fun isWhileEmpty() = whileStack.isEmpty()
}
