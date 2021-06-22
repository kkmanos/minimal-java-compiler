import java.io.InputStream;
import java.io.IOException;
import java.lang.Math;

public class parser {
    private int lookahead_token;
    private InputStream in;
    private String parsed_str; // just for debugging purposes

    public parser(InputStream input) throws IOException { // constructor
        in = input;
        parsed_str = "";
        lookahead_token = in.read(); // get first lookahead token
        parsed_str = parsed_str + (char)lookahead_token;
    }

    private void consume(int symbol) throws IOException, parse_error { // consumes the symbol
        if (lookahead_token != symbol)
            throw new parse_error();
        lookahead_token = in.read();
        parsed_str = parsed_str + (char)lookahead_token;
    }

    public void parse() throws IOException, parse_error {
        int res = exp(0);
        if (lookahead_token != '\n')
            throw new parse_error();
        else {
            System.out.printf("\nResult = %d\n", res);
        }
    }


    private int exp(int val) throws  IOException, parse_error {
        if (is_digit(lookahead_token) || lookahead_token == '(') {
            int term_result = term(0);
            int exp2_result = exp2(term_result);
            return exp2_result;
        }
        else {
            throw new parse_error();
        }
        //return -1;
    }

    private int exp2(int val) throws  IOException, parse_error {
        if (lookahead_token == '+') { // rule 2
            consume(lookahead_token); // read the next after '+'
            int term_result = term(0);
            int exp2_result = exp2(0);
            return val + term_result + exp2_result;
        }
        else if (lookahead_token == '-') { // rule 3
            consume(lookahead_token); // read the next after '-'
            int term_result = term(0);
            int exp2_result = exp2(0);
            return val - term_result + exp2_result;
        }
        else if (lookahead_token == '\n') { // rule 4
            return val;           // EOF
        }
        else if (lookahead_token == ')')
            return val; // epsilon

        throw new parse_error();
    }

    private int term(int val) throws  IOException, parse_error {
        // rule 5
        if (is_digit(lookahead_token) || lookahead_token == '(') {
            int factor_result = factor(0);
            int term2_result = term2(1);
            return (int) Math.pow(factor_result, term2_result); // factor ** term2
        }
        else {
            throw new parse_error();
        }
    }

    private int term2(int val) throws  IOException, parse_error {
        // rule 7
        if (lookahead_token == '+' || lookahead_token == '-' || lookahead_token == ')'
                                                             || lookahead_token == '\n') {
            return val; // epsilon, always returns 1 for no term change. Could be return 1;
        }
        // rule 6
        if (lookahead_token == '*') { // if have read a '*'
            consume('*');  // then read another one
            consume(lookahead_token);
            int factor_result = factor(0);
            int term2_result = term2(1);
            return (int) Math.pow(factor_result, term2_result); // return factor ** term2
        }

        throw new parse_error();
    }

    private int factor(int val) throws  IOException, parse_error {
        if (is_digit(lookahead_token)) { // rule 8
            int number_result = number("");
            //System.out.printf("\nNumber parsed: %d\n", number_result);
            return number_result;
        }
        else if (lookahead_token == '(') { // rule 9
            consume(lookahead_token);
            int exp_result = exp(0);
            consume(')'); // get the closing parenthesis
            return exp_result; // return result of (exp)
        }
        else
            throw new parse_error();
    }

    private int number(String s) throws  IOException, parse_error {

        if (is_digit(lookahead_token)) { // if is digit, then continue building s number
            int digit_char = eval_digit(lookahead_token);
            s = s + digit_char; // append to s
            consume(lookahead_token); // get next
            return number(s);
        }
        else { // if there are no other digits
            //System.out.printf("Integer: %s\n", s);
            return Integer.parseInt(s); // get the value of that string
        }
    }

//    private int exp2(int val) throws  IOException, parse_error {
//
//    }

    private boolean is_digit(int c) { return (c >= '0' && c <= '9'); }
    private int eval_digit(int c) { return c - '0'; } // convert from char to integer type
}