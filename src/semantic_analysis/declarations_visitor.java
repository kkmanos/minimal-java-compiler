package semantic_analysis;
import syntaxtree.*;
import visitor.*;
import symbol_table.*;
import offsets_table.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import error_print.error_printing;
public class declarations_visitor extends GJDepthFirst<String, String>{



    public symbol_table_t st = new symbol_table_t();
    public int err_count = 0;


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
        if (st.insert_class(classname, null) == false) { // add class to symbol table
            error_printing.print_err("Redeclaration of class \"" + classname + "\" was found");
            this.err_count++;
        }
		if (st.insert_method(classname, "main__", "void") == false) { // if failed
           error_printing.print_err("\nRe-declaration of method " + "main" + 
                        " was found.\nMethod overloading is not supported in minijava\n");
            this.err_count++;
            // no need to block here. proceed to print the rest of the errors
        }
 

        String var_declarationlist = n.f14.present() ? n.f14.accept(this, classname + "::" + "main__") : ""; // pass the new scope


        
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
        if (st.insert_class(classname, null) == false) { // add class to symbol table
            error_printing.print_err("Redeclaration of class \"" + classname + "\" was found");
            this.err_count++;
        }

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
        // "Notably, an A class can contain a field of type B, where B is defined later in the file. 
        if (st.lookup(supername) == null) { // is supername does not exist as a class
            error_printing.print_err("Type \"" + supername + "\" must be defined before class \"" + classname 
                            + "\" that extends the type");
            this.err_count++; // dont return, we need to check for errors in the member functions as well
        }

        if (st.insert_class(classname, supername) == false) { // add class to symbol table
            error_printing.print_err("Redeclaration of class \"" + classname + "\" was found");
            this.err_count++;
        }

        // Pass the classname as the scopename for the members
        n.f5.accept(this, classname);

        // Pass the  classname as the scopename for the member functions
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
        String myName = n.f2.accept(this, null);

        /**
         * INSERTIONS HERE
         */
       // insert method in argu scope (probably a class)
		if (st.insert_method(argu, myName+"__", myType) == false) { // if failed
           error_printing.print_err("In scope " + argu + " Re-use of identifier " + myName + " was found");
           this.err_count++;
           return "--TYPE_ERROR--";
        }
 

    
        // pass the method's scope name
        String param_type_list = n.f4.present() ? n.f4.accept(this, argu+"::"+myName+"__") : "";

        String var_declarationlist = n.f7.present() ? n.f7.accept(this, argu+"::"+myName+"__") : "";

        
       // check (prototype) if its an overload 
        method_t cur = (method_t) st.S.get(argu + "::" +  myName + "__");
        class_t current_class = (class_t) st.S.get(argu);
        String supername = current_class.supername; 
        if (supername != null) { // if has a superclass
           class_t superclass = (class_t) st.S.get(supername);
           String upper_method = st.id_lookup(supername + "::" + myName +"__");
           if (upper_method != null) { // if there is a method with the same id
                method_t m2 = (method_t) st.S.get(upper_method);
                if (!cur.same_prototype(m2)) {
                    error_printing.print_err("In scope " + argu + " Re-use of method name " + myName + " was found with different prototypes.");
                    this.err_count++;
                    return "--TYPE_ERROR--";

                }
            }
        }
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
        if (st.insert_param_type(argu, type) == false) { // insert in the param type list
            error_printing.print_err("In scope " + argu + " Identifier " + argu + " is not a method");
            this.err_count++;
            return "--TYPE_ERROR--";
        }


        if (st.insert_var(argu, name, type) == false)  { // insert in the var list of scope argu
            error_printing.print_err("In scope " + argu + " Redeclaration of variable \"" + name + "\" was found");
            this.err_count++;
            return "--TYPE_ERROR--";
        }
            

        return type;
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
        if (st.insert_var(argu, name, type) == false)  { // insert in the var list of scope argu
            error_printing.print_err("In scope " + argu + " Redeclaration of variable \"" + name + "\" was found");
            this.err_count++;
            return "--TYPE_ERROR--";
        }

        return type + " " + name;
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
