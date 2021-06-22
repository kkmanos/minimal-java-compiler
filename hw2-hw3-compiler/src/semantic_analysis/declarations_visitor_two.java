package semantic_analysis;
import syntaxtree.*;
import visitor.*;
import symbol_table.*;
import offsets_table.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import error_print.error_printing;
public class declarations_visitor_two extends GJDepthFirst<String, String>{



    symbol_table_t st; 
    public int err_count = 0;
    ArrayList<String> types = new ArrayList<>();



    public declarations_visitor_two(symbol_table_t symt) {
        this.st = symt;
        this.types.add("int");
        this.types.add("boolean");
        this.types.add("int[]");
        for (String key : this.st.S.keySet()) { // insert all classes in types arraylist
            if (this.st.S.get(key).get_type().equals("class_t")) 
                this.types.add(key);
        }
    }
    /**
     *  f0 -> MainClass()
     *  f1 -> ( TypeDeclaration() )*
     *  f2 -> <EOF>
     */
    @Override
    public String visit(Goal n, String argu) throws Exception {
        //String mainclass = n.f0.present() ? n.f0.accept(this, glob) : "";
        //String classes = n.f1.present() ? n.f1.accept(this, glob) : "";
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);


        return null;
    } 


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, String argu) throws Exception { // main class has only main(), no other function
        String classname = n.f1.accept(this, null);
        /**
         * INSERTIONS HERE
         */

        String var_declarationlist = n.f14.present() ? n.f14.accept(this, classname+"::main__") : ""; // pass the new scope


        
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String classname = n.f1.accept(this, argu);
        

        /**
         * INSERTIONS HERE
         */


        // Pass the classname as the scopename for the var declarations
        n.f3.accept(this, classname);
        
        
        // Pass the classname as the scopename for the method declarations
        n.f4.accept(this, classname); 
        

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        
        String classname = n.f1.accept(this, null);
        
        String supername = n.f3.accept(this, null);
        


        /**
         * INSERTIONS HERE
         */



        // Pass the classname as the scopename for the members
        n.f5.accept(this, classname);

        // Pass the  classname as the scopename for the member functiuons
        n.f6.accept(this,  classname);


        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"n.f4.present() ? n.f4.accept(this, st_method) : "";
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception {
        

        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null) + "__";

        /**
         * INSERTIONS HERE
         */
       // insert method in argu scope (probably a class)
		//st.insert_method(argu, myName, myType);

        //super.visit(n, st_method);
    
        // pass the method's scope name
        String param_type_list = n.f4.present() ? n.f4.accept(this, argu+"::"+myName) : "";

        String var_declarationlist = n.f7.present() ? n.f7.accept(this, argu+"::"+myName) : "";

        

        
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        String ret = n.f0.accept(this, argu);

        if (n.f1 != null) {
            ret += n.f1.accept(this, argu);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += "," + node.accept(this, argu);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception{
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);

        /**
         * INSERTIONS HERE
         */
        for (int i = 0; i < this.types.size(); i++) 
            if (type.equals(this.types.get(i))) // if found
                return type + " " + name;
        
        error_printing.print_err("In scope : " + argu + 
                ": Unknown type \"" + type +"\" was used for identifier: " + name
                + " in parameter list");
        this.err_count++;
        return "--TYPE_ERROR--";
    }


    /*
     * f0 -> Type() 
     * f1 -> Identifier()
     * f2 ->  ";"
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);

		/**
		 * INSERTIONS HERE
		 */
        
        for (int i = 0; i < this.types.size(); i++) 
            if (type.equals(this.types.get(i))) // if found
                return type + " " + name;
        
        error_printing.print_err("In scope : " + argu + 
                ": Unknown type \"" + type +"\" was used for identifier: " + name);
        this.err_count++;
        return "--TYPE_ERROR--";

    }

    @Override
    public String visit(ArrayType n, String argu) {
        return "int[]";
    }

    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    public String visit(IntegerType n, String argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, String argu) {
        return n.f0.toString();
    }
}