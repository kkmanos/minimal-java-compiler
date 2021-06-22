import java.io.IOException;

public class prog {
    public static void main(String[] args) {
        try {

            System.out.println("\nEnter an expression: ");
            parser p = new parser(System.in);
            p.parse();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
        catch(parse_error err){
            System.err.println(err.getMessage());
        }
    }
}
