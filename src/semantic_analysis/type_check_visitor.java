package semantic_analysis;
import syntaxtree.*;
import visitor.*;
import symbol_table.*;
import offsets_table.*;
import symbol_table.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import error_print.error_printing;



public class type_check_visitor extends GJDepthFirst<String, String>{

    symbol_table_t ST;
    String[] primitive_types = {"int", "boolean"}; // arrays are not primitive types
    ArrayList<String> types = new ArrayList<>(); // holds a list of already known types
    /**
     *  f0 -> MainClass()
     *  f1 -> ( TypeDeclaration() )*
     *  f2 -> <EOF>
     */

    public int type_errors = 0; // type_error counter




    public type_check_visitor(symbol_table_t st) {
        this.ST = st;
        for (String k : ST.S.keySet())
            if (ST.S.get(k).get_type().equals("class_t")) // add all types from declarations visitor
                types.add(k);
        types.add("int");
        types.add("int[]");
        types.add("boolean");
    }

    boolean is_type(String s) { // if s is a type, then return true, else return false
        for (int i = 0; i < this.types.size(); i++)
            if (s.equals(types.get(i)))
                return true;
        return false;
    }

    boolean is_primitive_type(String s) {
        for (int i = 0; i < this.primitive_types.length; i++)
            if (s.equals(primitive_types[i]))
                return true;
        return false;
    }

    /** 
     * f0 -> MainClass() 
     * f1 -> ( TypeDeclaration() )*
     */
    @Override
    public String visit(Goal n, String argu) throws Exception {
        //String mainclass = n.f0.present() ? n.f0.accept(this, glob) : "";
        //String classes = n.f1.present() ? n.f1.accept(this, glob) : "";
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);

        if (this.type_errors != 0)
            return "TYPE ERRORS: " + this.type_errors;

        return "NO TYPE ERRORS";
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

        n.f15.accept(this, classname + "::main__");
        
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
        
        String methods = n.f4.accept(this, classname); // pass the scopename

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
        
        String has_super = n.f3.accept(this, null);
        

        /**
         * INSERTIONS HERE
         */

        String methods = n.f6.accept(this, classname);

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
        String myName = n.f2.accept(this, null)+"__";

        /**
         * CODE HERE
         */
        // pass the current scope + method name to the statements
        String statement_list = n.f8.present() ? n.f8.accept(this, argu + "::" + myName) : "";
        String ret_type = n.f10.accept(this, argu + "::" + myName); // pass scope to the return expression
        //String expected_ret_type = ST.lookup(argu + "::" + myName, "method_t");
        String fun_ptr = ST.vtable_lookup(argu, myName); // get the function fullname from the vtable
        String expected_ret_type = ((method_t) ST.S.get(fun_ptr)).ret_type;

        if (!ret_type.equals(expected_ret_type)) { // if return type is not matching the prototype
            String temp = new String(ret_type);
            // in case they are different types, check if ret_type is an extension of exprected_ret_type
            while ((temp = ST.get_parent_class(temp)) != null) {
                if (expected_ret_type.equals(temp)) { // if type was found, then return id_type
                    return null;
                }
            }
            String msg = "In scope " + argu+"::"+myName + " Wrong type was returned\n";
            msg += "\tWas expecting type \"" + expected_ret_type  + "\"";
            error_printing.print_err(msg);
            this.type_errors++;
            return "--TYPE_ERROR--";
        }
        return null;
    }






    /**
     * f0 -> "while" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, String argu) throws Exception {
        String expr_type = n.f2.accept(this, argu);

        if (expr_type.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";

        if (!expr_type.equals("boolean")) {
            error_printing.print_err("In scope: " + argu + " , expression of \"while\" statement must be of type \"boolean\"");
            this.type_errors++;
            // dont return something. we need to parse the Statement as welll
        }
        

        n.f4.accept(this, argu); //  no type-check , its a Statement

        return null; // no need to return a type because its a statement
    }

    /** 
     * f0 -> "System.out.println" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, String argu) throws Exception {
        String expr_type = n.f2.accept(this, argu);
        if (expr_type.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";

        if (!expr_type.equals("int")) {
            error_printing.print_err("In scope: " + argu + ", expression inside println should be of type \"int\". Instead \"" + expr_type + "\" was given");
            this.type_errors++;
        }
        return null; // no need to return a type, because its a statement
    }

    /**
     * f0 -> "if" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> Statement() 
     * f5 -> "else" 
     * f6 -> Statement()
     */
    @Override
    public String visit(IfStatement n, String argu) throws Exception {
        String expr_type = n.f2.accept(this, argu);

        // we dont need to block here, because we want to type-chek the statements
        // as well, to help the user see more errors
        //if (expr_type.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";

        if (!expr_type.equals("boolean")) {
            error_printing.print_err("In scope: " + argu + " Expression of \"if\" statement must be of type \"boolean\"");
            this.type_errors++;
            //return "--TYPE_ERROR--"; // must not block here
        }
        

        n.f4.accept(this, argu); //  no need block errors, because they are statements. 
        n.f6.accept(this, argu); //  same
        return null; // no need to return a type because its a statement
    }



    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String id = n.f0.accept(this, argu);
        String id_type = ST.lookup(argu + "::" + id);
        String rvalue_type = n.f2.accept(this, argu);

        if (rvalue_type.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";
        if (id_type == null) {
            error_printing.print_err("In scope " + argu + " Identifier \"" + id + "\" has not been defined");
            this.type_errors++;
            return "--TYPE_ERROR--";
        }
       
        String s = ST.id_lookup(argu + "::" + id);
        var_t var = ((var_t) ST.S.get(s));
        String temp = new String(rvalue_type);

         if ( var.dynamic_type != null && var.dynamic_type.equals(rvalue_type))
            return var.dynamic_type;
        
        //System.out.println("fullname = " + argu +"::"+id + "   " + rvalue_type);
        if (id_type.equals(rvalue_type) &&  is_primitive_type(id_type)) // if types are same and are primitive
            return id_type;


        if (id_type.equals(rvalue_type) && !is_primitive_type(rvalue_type)) {
            var.dynamic_type = new String(rvalue_type);
            return id_type;
        }


        
        // in case they are different types, check if rvalue is an extension of id_type
        while ((temp = ST.get_parent_class(temp)) != null) {
            if (id_type.equals(temp)) { // if type was found, then return id_type
                var_t v = (var_t) ST.S.get(argu + "::" + id);
                v.dynamic_type = new String(rvalue_type);
                //error_printing.print_err(((var_t)ST.S.get(argu + "::" + id)).dynamic_type);
                return rvalue_type;
            }
        }
        if (var.dynamic_type == null) { // if it is uninitialized, then set the new dynamic type
            var.dynamic_type = new String(rvalue_type);
        }

        // if none of the above cases return a type, then throw exception
        String msg = "In scope " + argu + " Assignment was done with different types at symbol: " + id + ".\nIdentifier " + id + " cannot be converted into type " + rvalue_type ;
        error_printing.print_err(msg);
        this.type_errors++;
        return "--TYPE_ERROR--";
        
    }


    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     */
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        
        String id = n.f0.accept(this, argu);
        String idtype = ST.lookup(argu + "::" + id);
        String brac_type = n.f2.accept(this, argu);
        String right_expr = n.f5.accept(this, argu);
        if (brac_type.equals("--TYPE_ERROR--") || right_expr.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";


        if (idtype == null) {
            error_printing.print_err("==In scope "+argu+ " Identifier \"" + id + "\" has not been defined");
            this.type_errors++;
            return "--TYPE_ERROR--";
        }
        if (idtype.equals("int[]")) {
            if (brac_type.equals("int"))
                if (right_expr.equals("int")) {
                    return "int";
                }
                else {
                    error_printing.print_err("In scope " + argu + " Left side of array assignment operation is of wrong type");
                    this.type_errors++;
                    return "--TYPE_ERROR--";
                }
            else {
                this.type_errors++;
                error_printing.print_err("In scope " + argu  + " Index expression inside [ ] in array assignment, must be of type int\n");
                return "--TYPE_ERROR--";
            }
        }
        else {
            this.type_errors++;
            String msg = "In scope " + argu + "Assignment was done with different types at symbol: " + id;
            error_printing.print_err(msg);
            return "--TYPE_ERROR--";
        }
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(AndExpression n, String argu) throws Exception {
        String p1 = n.f0.accept(this, argu);
        String p2 = n.f2.accept(this, argu);
        if (p1.equals("--TYPE_ERROR--") || p2.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";

        if (!p1.equals("boolean")) {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " Left operand of operator \"&&\" must be of type \"boolean\"");
            return "--TYPE_ERROR--";
        }
        if (!p2.equals("boolean")) {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " Right operand of operator \"&&\" must be of type \"boolean\"");
            return "--TYPE_ERROR--";
        }
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        String p1_type = n.f0.accept(this, argu);
        String p2_type = n.f2.accept(this, argu);

        if (p1_type.equals("--TYPE_ERROR--") || p2_type.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";
        if (p1_type.equals("int") && p2_type.equals("int")) { // if both PrimExprs are "int"
            return "boolean"; // comparison is of type boolean
        }
        else {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " Bad operand types in comparison operator \"<\"");
            return "--TYPE_ERROR--";
        }
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception {
        String p1 = n.f0.accept(this, argu);
        String p2 = n.f2.accept(this, argu);

        if (p1.equals("--TYPE_ERROR--") || p2.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";

        if (p1.equals("int")) {
            if (p2.equals("int"))
                return "int";
            else {
                this.type_errors++;
                error_printing.print_err("In scope " + argu + " Right operand of operator \"+\" must be of type int");
                return "--TYPE_ERROR--";
            }
        }
        else { 
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " Left operand of operator \"+\" is of wrong type");   
            return "--TYPE_ERROR--";
        }
        
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception {
        String p1 = n.f0.accept(this, argu);
        String p2 = n.f2.accept(this, argu);

        if (p1.equals("--TYPE_ERROR--") || p2.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";

        if (p1.equals("int")) {
            if (p2.equals("int"))
                return "int";
            else {
                this.type_errors++;
                error_printing.print_err("In scope " + argu + " Right operand of operator \"-\" must be of type int");
                return "--TYPE_ERROR--";
            }
        }
        else {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " Left operand of operator \"-\" is of uknown type");        
            return "--TYPE_ERROR--";
        }
    }


    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception {
        String p1 = n.f0.accept(this, argu);
        String p2 = n.f2.accept(this, argu);

        if (p1.equals("--TYPE_ERROR--") || p2.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";

        if (p1.equals("int")) {
            if (p2.equals("int"))
                return "int";
            else {
                this.type_errors++;
                error_printing.print_err("In scope " + argu + " Right operand of operator \"*\" must be of type int");
                return "--TYPE_ERROR--";
            }
        }
        else {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " Left operand of operator \"*\" is of uknown type");
            return "--TYPE_ERROR--";
        }
    
    }       

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 ->  PrimaryExpression() 
     * f3 -> "]"
     */
    @Override 
    public String visit(ArrayLookup n, String argu) throws Exception {
        String p1 = n.f0.accept(this, argu);
        String p2 = n.f2.accept(this, argu);

        if (p1.equals("--TYPE_ERROR--") || p2.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";

        if (!p1.equals("int[]")) {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " Array lookup cannot be used in non array-type expressions");
            return "--TYPE_ERROR--";
        }
        if (!p2.equals("int")) {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " Index used in array lookup must be of type \"int\"");
            return "--TYPE_ERROR--";
        }
        return "int"; // array lookup returns "int"
    }



    /** 
     * f0 -> PrimaryExpression() 
     * f1 -> "." 
     * f2 -> Identifier() 
     * f3 -> "(" 
     * f4 -> ( ExpressionList() )? 
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, String argu) throws Exception {
        String p_type = n.f0.accept(this, argu);
        String id = n.f2.accept(this, argu)+"__";

        if (p_type.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";
        if (is_primitive_type(p_type)) { // if is a primitive type
            error_printing.print_err("In scope " + argu + " :Primitive types cannot call methods");
            this.type_errors++;
            return "--TYPE_ERROR--";
        }
        String fullname = p_type + "::" + id;
        //System.out.println("fullname = " + fullname);
        String ret_type;
        if (ST.vtable_lookup(p_type, id) == null) {
            this.type_errors++;

            error_printing.print_err("In scope " + argu + " :Identifier \""+ id + "\" is not a method to be called");
            return "--TYPE_ERROR--";
        }
        else {
            fullname = ST.vtable_lookup(p_type, id);
            method_t m = (method_t) ST.S.get(fullname);
            ret_type = m.ret_type;
        }


        //error_printing.print_err("fullname = " + fullname);
        String s = n.f4.accept(this, argu);
        String[] arg_list_types = {}; //init with empty
        if (s != null) // if args are given, then
            arg_list_types = s.split(",");

        
        method_t m = ST.get_method(fullname);
        ArrayList<String> paramlist_types = m.paramlist_types;

        int typefound = 0; // used only as a flag for the type check for inheritance

        if (arg_list_types.length != 0) { // if args where given
            // if we have correct number of args
            if (paramlist_types.size() == arg_list_types.length) { 
                // for each argument and param type
                for (int i = 0; i < arg_list_types.length; i++) {
                    // if at least one is different, then
                    if (!(paramlist_types.get(i).equals(arg_list_types[i]))) {
                        // in case they are different types, check if argument type is an extension of the parameter type
                        String arg_type = new String(arg_list_types[i]);
                        while ((arg_type = ST.get_parent_class(arg_type)) != null) {
                            if (paramlist_types.get(i).equals(arg_type)) {// if type was found, then this arg type is correct
                                typefound = 1;
                                break;
                            }
                        }
                        if (typefound == 1) {
                            typefound = 0; // reset 
                            continue;  // if type was found because of inheritance, then thats not an error
                        }
        
                        String msg = "In scope " + argu + " :Wrong type of argument number " + i 
                            + ". Was expecting type: " + paramlist_types.get(i);
                        msg += "\nPrototype of method " + id + " is:\n";

                        error_printing.print_err(msg);
                        m.print(); // print prototype of m
                        this.type_errors++;
                        return "--TYPE_ERROR--";
                    }
                }
            }
            else {
                String msg = "In scope " + argu + " :Wrong number of args in method call " + fullname;
                msg += "Prototype of method " + id + " is:\n";
                error_printing.print_err(msg);
                m.print(); // print prototype of m
                this.type_errors++;
                return "--TYPE_ERROR--";
            }
        }
        else {
            if (paramlist_types.size() != 0) {

                error_printing.print_err("In scope " + argu + " :Wrong number of arguments. Method should not have any arguments");
                m.print(); // print prototype of m
                this.type_errors++;
                return "--TYPE_ERROR--";
            }
        }
        
        
        return ret_type; // the return type of the method is the type of the MessageSend

        
    }


    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "." 
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception {
        String ptype = n.f0.accept(this, argu);
        if (ptype.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";
        if (ptype.equals("int[]")) {
            return "int"; // length is of type "int"
        }
        else {
            this.type_errors++;
            error_printing.print_err("Object used in \".length\" operation, cannot be dereferenced");
            return "--TYPE_ERROR--";
        }
    }


    /**
     * f0 -> IntegerLiteral() | TrueLiteral() | 
     *      FalseLiteral() | Identifier() | ThisExpression() 
     *       | ArrayAllocationExpression() | 
     *       AllocationExpression() | NotExpression() | BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, String argu) throws Exception {
        String p = n.f0.accept(this, argu);


        if (p.equals("--TYPE_ERROR--")) return "--TYPE_ERROR--";
        // literals, BracketExpr, AllocExpr and NotExpr will finish in the following 
        // if (is_type(p)) because their type has already been defined
        // Only ThisExpr() and Identifier() have not been defined yet, and that's
        // why they return a symbol and not a type. In this Override function, we get
        // the identifier's type and this's type.
        if (is_type(p)) // if we already know its type
            return p;
        if (p.equals("this")) { // if its a ThisExpression
            String[] sp = argu.split("::");
            // If "this" was used in A::foo scope, then we return 
            return sp[0]; // return the class type in which it was used
        }

        // in case this is an Identifier
        String ptype = ST.lookup(argu + "::" + p); // search in the Symbol table for a varname
        if (ptype != null) { // if found
            if (is_primitive_type(ptype))
                return ptype;
            var_t v = (var_t) ST.S.get(ST.id_lookup(argu+"::"+p));
            if (v.dynamic_type == null) 
                return v.type;
            else {
                //System.out.println("Dyn type = " + v.dynamic_type);
                return v.dynamic_type;
            }
        }
        else {
            this.type_errors++;
            error_printing.print_err("In scope "+argu+ " Identifier \"" + p + "\" has not been defined");
            return "--TYPE_ERROR--";
        }
    }


    /** 
     * f0 -> "!" 
     * f1 -> PrimaryExpression()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception {
        String prim_expr_type = n.f1.accept(this, argu);
        if (!prim_expr_type.equals("boolean")) {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " :Bad operand type of expression in Not operator \"!\"");
            return "--TYPE_ERROR--";
        }
        return "boolean"; // NotExpression is always of type"boolean"
    }


    /**
     * f0 -> "(" 
     * f1 -> Expression() 
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu); // returns the type of the expression
    }

    /**
     * f0 -> "new" 
     * f1 -> Identifier() 
     * f2 -> "(" 
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        String id = n.f1.accept(this, argu);
        String idtype = ST.lookup(id); // search for a type "id"
        if (idtype == null) {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " :Unknown type \"" + id  + "\" was used for allocation in new operator");
            return "--TYPE_ERROR--";
        }
        return idtype; // a "new" operation returns an object of type idtype

    }

    /**
     * f0 -> "new" 
     * f1 -> "int" 
     * f2 -> "[" 
     * f3 -> Expression() 
     * f4 -> "]"
     */
    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        String exprtype = n.f3.accept(this, argu);
        if (!exprtype.equals("int")) {
            this.type_errors++;
            error_printing.print_err("In scope " + argu + " Expression in array allocation must be of type \"int\"");
            return "--TYPE_ERROR--";
        }
        return "int[]"; // array allocation  is always returns an object of type "int[]"
    }

    // standards:
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

    /**
     * LITERALS must return their type
     */
    @Override
    public String visit(TrueLiteral n, String argu) {
        return "boolean"; 
    }
    @Override
    public String visit(FalseLiteral n, String argu) {
        return "boolean"; 
    }
    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        //error_printing.print_err("Integer literal parsed: " + n.f0);
        return "int"; 
    }

    @Override
    public String visit(ThisExpression n, String argu) {
        return "this"; 
    }

    @Override
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu).toString(); // a type
    }

    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        String ret = n.f0.accept(this, argu);
        if (n.f1 != null) {
            ret += n.f1.accept(this, argu);
        }
        return ret;
    }

    @Override
    public String visit(ExpressionTail n, String argu) throws Exception {
        String ret = "";
        for (Node node: n.f0.nodes) {
            ret += "," + node.accept(this, argu);
        }
        return ret;
    }


    @Override
    public String visit(ExpressionTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu).toString();
    }
    

}