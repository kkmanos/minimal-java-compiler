JAR_DIR=..


all: compile

compile:
	jflex scanner.flex
	java -jar $(JAR_DIR)/java-cup-11b.jar -interface -parser Parser parser.cup
	javac -cp $(JAR_DIR)/java-cup-11b-runtime.jar *.java

execute:
	@java -cp $(JAR_DIR)/java-cup-11b-runtime.jar:. Driver

clean:
	rm -f *.class *~ Main.java Parser.java Scanner.java sym.java
