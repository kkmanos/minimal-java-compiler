

Homework 1 - LL(1) Calculator Parser - Translator to Java
Part 1

For the first part of this homework you should implement a simple calculator. The calculator should accept expressions with the addition, subtraction, and exponentiation operators, as well as parentheses. The grammar (for multi-digit numbers) is summarized in:

exp -> num | exp op exp | (exp)

op -> + | - | **

num -> digit | digit num

digit -> 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9

You need to change this grammar to support priority between the operators, to remove the left recursion for LL parsing, etc.

This part of the homework is divided in two tasks:

For practice, you can write the FIRST+ & FOLLOW sets for the LL(1) version of the above grammar. In the end you will summarize them in a single lookahead table (include a row for every derivation in your final grammar). This part will not be graded.

You have to write a recursive descent parser in Java that reads expressions and computes the values or prints "parse error" if there is a syntax error. You don't need to identify blank spaces. You can read the symbols one-by-one (as in the C getchar() function). The expression must end with a newline or EOF.

Your parser should read its input from the standard input (e.g., via an InputStream on System.in) and write the computed values of expressions to the standard output (System.out). Parse errors should be reported on standard error (System.err).

Part 2
In the second part of this homework you will implement a parser and translator for a language supporting string operations. The language supports the concatenation (+) operator over strings, function definitions and calls, conditionals (if-else i.e, every "if" must be followed by an "else"), and the following logical expressions:

is-prefix-of (string1 prefix string2): Whether string1 is a prefix of string2.
is-suffix-of (string1 suffix string2): Whether string1 is a suffix of string2.
All values in the language are strings.

The precedence of the operator expressions is defined as: precedence(if) < precedence(concat).

Your parser, based on a context-free grammar, will translate the input language into Java. You will use JavaCUP for the generation of the parser combined either with a hand-written lexer or a generated-one (e.g., using JFlex, which is encouraged).

You will infer the desired syntax of the input and output languages from the examples below. The output language is a subset of Java so it can be compiled using javac and executed using Java or online Java compilers like this, if you want to test your output.

There is no need to perform type checking for the argument types or a check for the number of function arguments. You can assume that the program input will always be semantically correct.

Note that each file of Java source code you produce must have the same name as the public Java class in it. For your own convenience you can name the public class "Main" and the generated files "Main.java". In order to compile a file named Main.java you need to execute the command: javac Main.java. In order to execute the produced Main.class file you need to execute: java Main.

To execute the program successfully, the "Main" class of your Java program must have a method with the following signature: public static void main(String[] args), which will be the main method of your program, containing all the translated statements of the input program. Moreover, for each function declaration of the input program, the translated Java program must contain an equivalent static method of the same name. Finally, keep in mind that in the input language the function declarations must precede all statements.

As with the first part of this assignment, you should accept input programs from stdin and print output Java programs to stdout.



    PART 1
    ~~~~~~

    1.1. Compilation & Execution
    

    compile and execute at part1/src directory
    
    	$ make compile && make execute



    1.2. Grammar

    Ο κώδικας του parser αναπτύχθηκε με βάση την παρακάτω
    γραμματική:

    #1 expr -> term Expr2
    #2 Expr2 -> +  term Expr2
    #3         |  -  term Expr2
    #4         |  ε
    #5 term -> Factor Term2
    #6 Term2 ->  **  Factor Term2
    #7         | ε
    #8 Factor -> Number
    #9          | (expr)


    lookup table:
           number | +  | -  | ** | (  | )  | EOF
    expr     #1   |    |    |    | #1 |    |
    expr2         | #2 | #3 |    |    | #4 | #4
    term     #5   |    |    |    | #5 |    |
    term2         | #7 | #7 | #6 |    | #7 | #7
    factor   #8   |    |    |    | #9 |    |    

	
	Note: No space or tab characters are accepted.


    PART 2
    ~~~~~~

	2.1. Compilation & Execution
	
    compile and execute at part2/src directory
    
    	$ make compile && make execute
    
    
	Finish input, with Ctrl-D
