# jack2c
## An extended Jack compiler

This implementation delivers some extensions over the compiler described in the
Nand2Tetris book (http://nand2tetris.org/book.php):

1. _**named**_ constants
2. _**character**_ constants
3. _**break**_ statements

Jack2c accepts .jack source files written for the supplied JackCompiler(v2.5).

CommandLine parameters allow the generation of .vm files with embedded
comments to simplify the testing in a VMEmulator:

  - symbol tables at class and subroutine levels
  - jack statements (to see some structure in large .vm files)
  - enhanced explanations (of more complicated constructions)
