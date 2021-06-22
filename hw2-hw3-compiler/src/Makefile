

ll=${$(file)/.java/.ll}
java_suffix=.java
ll_suffix=.ll
JARS=../
out_suffix=.out
llfile= $(subst $(java_suffix),$(ll_suffix),$(file))
outfile= $(subst $(java_suffix),$(out_suffix),$(file))
all: clean compile

compile:
	java -jar $(JARS)/jtb132di.jar -te minijava.jj
	java -jar $(JARS)/javacc5.jar minijava-jtb.jj
	javac symbol_table/*.java
	javac offsets_table/*.java
	javac semantic_analysis/*.java
	javac error_print/*.java
	javac Main.java

exec:
	java Main $(file)
	clang -o $(outfile) $(llfile)
	$(outfile)

clean:
	rm -f *.class *~ symbol_table/*.class offsets_table/*.class semantic_analysis/*.class error_print/*.class ./out src/out llvm_structs/*.class

