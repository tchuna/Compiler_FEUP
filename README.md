# Compiler of the Java-- language to Java Bytecodes

### SUMMARY 
Our tool contains a parser for the Java-- language with syntatic and semantic error detection (and recovery). It begins by reading a jmm file and generating the respective AST, anotating the necessary information for the next stages in its nodes. The syntatic analysis is done at this time as well. 
The semantic analysis comes next (after generating the symbol tables necessary). If no errors are detected, it is generated jasmin code and saved in the output file specified. 

### EXECUTION
There are to ways to execute our tool:

* Through the .jar file: java -jar JavaMM.jar <input_file> <output_file>
* Through the project, from the /src folder: java compiler.JavaMMMain <input_file> <output_file>

The arguments indicate the file from which to read jmm code and where to save the jasmin code generated, respectively.

In order to run and see the AST alone, it is needed to execute the following commands regarding the files in the **ast_runnable** folder:

* <local_path>\javacc-6.0\bin\open_cmd.bat
* jjtree JavaMM.jjt
* javacc JavaMM.jj
* javac JavaMM.java
* java JavaMM <input_file>

The reason for this is that the "same" files in the ast package were altered in order to include package directives. Unfortunately, this prevents these files from being run in the same way.

In order to generate the Java class files, the Jasmin.jar is provided in the JavaMM/src/jasmin folder. To run the files generated, simply use: java <class_file>.

### DEALING WITH SYNTACTIC ERRORS
The compiler recovers from syntatic errors detected on the conditions of while loops, skipping to the end of that condition and continuing the analysis from there.

### SEMANTIC ANALYSIS
The compiler detects most of the semantic errors in regular Java, although only in the functions/variables of the class/file received as input. When dealing with external classes, the compiler assumes that function calls are executed correctly and return the expected values. 
Some notable exemples of the semantic rules taken into account (although not restricted to) are:

* Variable initialization before use.
* Variable/function declaration before initialization/use.
* Local functions argument type and number check when invoking said functions.
* Local functions return value check.

### INTERMEDIATE REPRESENTATIONS
The compiler takes the AST generated (as well as the symbol tables) in order to perform the code generation.

### CODE GENERATION: 
In order to generate the Jasmin code, the compiler iterates through the AST nodes in a depth first approach, evaluating their attributes as well as their relations with each other in order to pick the correct instructions. Eventual problems **may or may not** include missing POP instructions in some specific (although rare) situations.

### OVERVIEW

The AST is built in accordance with the JJTree file containing the syntatic rules, using JavaCC and JavaC.
The semantic analysis and code generation is done through the examination and evaluation of the different nodes and their relations to each other, while iterating over the tree in a depth-first like approach.
This last stage generates machine code accepted by the Jasmin tool which in turn, when run, generates the class files necessary to run the code.

### PROS

* High level of modularity and separation of code, making the introduction of new functionalities relatively easy.
* Custom messages in every module help to quickly pinpoint errors.
* Clean and easy to understand AST. 

### CONS

* Although the code is split into modular functions, most of it is in the same file, making the search for specific ones hard sometimes.

* Lack of optimizations in code generation.
