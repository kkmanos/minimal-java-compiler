package error_print;

public class error_printing {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";


    public static void print_err(String msg) {
        System.out.println(ANSI_RED + "error: " + ANSI_RESET + msg);
    }


}
