

# Minimal Java Compiler 
#### Koukoularis Emmanouil - sdi1700262




### Compilation & Testing	
	
For compilation, on the src/ directory, execute the following command:

```	
make compile
```	
	
To test the compiler, run the following command which will compile and execute
the program:

```	
make exec file=/path/to/file_to_compile_and_exec.java
```

Example:
```
make exec file=./tests/main.java
```