import syntaxtree.*;
import visitor.*;

import llvm_structs.*;
import java.io.*;
import java.util.ArrayList;

import offsets_table.offsets_table_t;

import semantic_analysis.declarations_visitor;

import semantic_analysis.declarations_visitor_two;
import symbol_table.*;
import semantic_analysis.type_check_visitor;
public class Main {

    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main [file1] [file2] ... [fileN]");
            System.exit(1);
        }

        ArrayList<String> summary = new ArrayList<String>();
        FileInputStream fis = null;


        try{
            for (int i = 0; i < args.length; i++) {
                System.out.println("\n\n-- file name: " + args[i]);
                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();

                System.err.println("Program parsed successfully.");

                int decl_errors = 0;
                // call first visitor for declarations storing
                declarations_visitor decl_visitor = new declarations_visitor();
                root.accept(decl_visitor, ""); // call with "" for global scope
                decl_errors += decl_visitor.err_count;


                // call 2nd visitor for declaration type existence check
                declarations_visitor_two decl_visitor2 = new declarations_visitor_two(decl_visitor.st);
                root.accept(decl_visitor2, "");
                decl_errors += decl_visitor2.err_count;

                int type_errors = 0;
                if (decl_errors == 0) { // if no declaration errors occured
                    type_check_visitor tcheck_visitor = new type_check_visitor(decl_visitor.st);
                    root.accept(tcheck_visitor, ""); // call with "" for global scope
                    type_errors = tcheck_visitor.type_errors;
                }
                offsets_table_t offsets = null; 
                if (type_errors == 0 && decl_errors == 0) {
                    offsets = new offsets_table_t();
                    summary.add(args[i] + ANSI_GREEN + "\n\t\t--Declaration errors: " + decl_errors
                                + "\n\t\t--Type-check errors: " + type_errors + ANSI_RESET);
                    
                    offsets.init_and_print(decl_visitor.st);
                }
                else {
                    System.out.println("--Number of declaration errors: " + decl_errors);
                    System.out.println("--Number of type-check errors: " + type_errors);
                    summary.add(args[i] + ANSI_RED + "\n\t\t--Declaration errors: " + decl_errors
                                + "\n\t\t--Type-check errors: " + type_errors + ANSI_RESET);
                    continue;

                }


				String llfile = args[i].replace(".java",".ll");
				Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(llfile), "utf-8"));    
                llvm_vtable_visitor vt_visitor = new llvm_vtable_visitor(writer, offsets, decl_visitor.st);
                root.accept(vt_visitor, "");
                writer.write("\n\n");

                llvm_visitor ll_visitor = new llvm_visitor(writer, offsets, decl_visitor.st, vt_visitor.c);
                root.accept(ll_visitor, "");
                writer.close();
            }

            System.out.println("\n**** SUMMARY ****");
            for (int i = 0; i < summary.size(); i++) {
                System.out.println();
                System.out.println(summary.get(i));
            }
        }
        catch(ParseException ex){
            System.out.println(ex.getMessage());
        }
        catch(FileNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        finally{
            try{
                if(fis != null) fis.close();
            }
            catch(IOException ex){
                System.err.println(ex.getMessage());
            }
        }
    }
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
}


