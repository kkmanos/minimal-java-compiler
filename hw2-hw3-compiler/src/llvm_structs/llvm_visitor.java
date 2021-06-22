package llvm_structs;
import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

import llvm_structs.collocations_t;
import offsets_table.offsets_table_t;
import semantic_analysis.*;
import symbol_table.*;
import java.io.*;

public class llvm_visitor extends GJDepthFirst <String,String> {

    Writer writer = null;
    offsets_table_t offset_table = null;
    symbol_table_t st = null;
    boolean lflag = false; // lgenerate
    collocations_t c = null;  // stores {key= fullname : value= "%_number"} tuples 
    public llvm_visitor(Writer w, offsets_table_t offt, symbol_table_t ST, collocations_t C) throws Exception {
        writer = w;
        offset_table = offt;
        st = ST;
        c = C;
    }

    public static boolean isprimitive(String type) {
        return (type.equals("int") || type.equals("int[]") || type.equals("boolean"));
    }


    public void emit(String to_emit) throws Exception {
        this.writer.write(to_emit + "\n");
    }

    public String allocate_var(String identifier, String type) {
        return identifier + " = alloca " + collocations_t.real_type(type);
    }

    public String allocate_object(String type_to_alloc) throws Exception { // type_to_alloc is A  in "new A()" expression
        emit("\n; Allocating object");
        String to_write = new String("");
        // There is no problem to pass "i8*" directly, since type i8* cannot be defined by the programmer
        // so still "i8*" will be the real_type

        int struct_size = 8 +  this.st.get_sizeof_struct(type_to_alloc); // we add 8 for the vtable ptr
        String mem = this.c.new_temp(type_to_alloc); // store it with dynamic type
        to_write += String.format("%s = call i8* @calloc(i32 1, i32 %d)\n", mem, struct_size);

        String bitcast = this.c.new_temp();
        to_write += String.format("%s = bitcast i8* %s to i8***\n", bitcast, mem);

        String elemptr = this.c.new_temp();
        String vtable_name = "@."+type_to_alloc + "_vtable";
        int n_methods = ((class_t)this.st.S.get(type_to_alloc)).vtable.vt.size();
        to_write += String.format("%s = getelementptr [%d x i8*], [%d x i8*]* %s, i32 0, i32 0\n",
                                       elemptr, n_methods, n_methods, vtable_name);
        to_write += String.format("store i8** %s, i8*** %s\n", elemptr, bitcast);
        this.emit(to_write);
        return mem; // return the identifier holding the allocated memory for the object 

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


        emit("\ndefine " + "i32" + " " + 
                        "@main" + "(" + ") {");
        n.f14.accept(this, classname + "::" + "main__");
        n.f15.accept(this, classname + "::" + "main__");
        emit("ret i32 0");
        emit("}");

        
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
        n.f4.accept(this, classname); // pass the classname as scope
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
        String classname = n.f1.accept(this, argu);
        n.f6.accept(this, classname); // pass the classname as scope
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
    public String visit(MethodDeclaration n, String argu) throws Exception {
        String type = n.f1.accept(this, argu);
        String methodid = n.f2.accept(this, argu) + "__";
        String methodfullname = argu + "::" + methodid;
        String param_list = n.f4.present() ? n.f4.accept(this, methodfullname) : "";
        if (param_list.equals("")) {
            emit("define " + collocations_t.real_type(type) + " " + 
                        c.new_fun(methodfullname) + "(i8* %this) {");
        }
        else {
            String converted_param_list = new String(param_list);
            String[] conv_arr = converted_param_list.split(",");
            String toprint = new String(""); // buffer for the param list print inside parenthesis
            for (int i = 0; i < conv_arr.length; i++) {
                String[] conv_param = conv_arr[i].split(" ");
                toprint += collocations_t.real_type(conv_param[0]);

                String tag = c.new_tag(conv_param[0], methodfullname+"::"+conv_param[1]);
                toprint += " " + tag + "_param" ; // parameters endswith with "_param"
                if (i != conv_arr.length -1) toprint += ",";

            }


            emit("define " + collocations_t.real_type(type) + " " + 
                        c.new_fun(methodfullname) + "(i8* %this, "+ toprint +") {");
            String[] params = toprint.split(",");
            for (int i = 0; i < params.length; i++) {
                params[i] = params[i].replace("_param", ""); // remove the "_param" from the parameter
                String[] arr = params[i].split(" ");
                emit(arr[1] + " = alloca " + arr[0]);
                emit(String.format("store %s %s, %s* %s", arr[0], arr[1] + "_param" ,arr[0], arr[1]));
            }
        }
        
        //emit(n.f7.present() ? n.f7.accept(this, methodfullname) : "");
        //emit(n.f8.present() ? n.f8.accept(this, methodfullname) : "");
        n.f7.accept(this, methodfullname);
        n.f8.accept(this, methodfullname);
        emit("ret " + collocations_t.real_type(type) + " " + n.f10.accept(this, methodfullname));
        emit("}");
        emit("\n\n");
        return null;
    }

    /*
     * f0 -> Identifier() 
     * f1 -> "=" 
     * f2 -> Expression() 
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, String argu) throws Exception {
        emit("\n\n; assignment statement follows");
        String identifier = n.f0.accept(this, argu);

        String expr_res = n.f2.accept(this, argu);
        //String expr_val = c.new_temp();
        //var_t v = (var_t)st.S.get(argu + "::" + identifier);
        //System.out.println("varid = " + argu + "::"+identifier);
        String fullname = st.id_lookup(argu + "::" + identifier);

        var_t v = (var_t) st.S.get(fullname);
        String id_type = new String();
        if (!fullname.equals(argu+"::"+identifier)) { // if different, then identifier is a member variable
            String classname = argu.split("::")[0]; // get the classname from A::foo
            // we add 8 because, the start of an object is the pointer to the vtable
            var_t member = (var_t) st.S.get(fullname);
            int offset = 8 + this.offset_table.get_offset(fullname, this.st);

            String t = collocations_t.real_type(member.type); // store real type
            String ptr_idx = c.new_temp();
            emit(String.format("%s = getelementptr i8, i8* %s, %s %s", ptr_idx, "%this", "i32", offset));
            String casted_tag = c.new_temp(member.type);

            emit(String.format("%s = bitcast i8* %s to %s*", casted_tag, ptr_idx, t));

            emit(String.format("store %s %s, %s* %s", t, expr_res, t, casted_tag));

        }
        else {
            id_type = collocations_t.real_type(v.type);
            emit(String.format("store %s %s, %s* %s", id_type, expr_res, id_type, c.get_tag(fullname)));
            String expr_type = c.get_tag_type(expr_res);
            String id_tag = c.get_tag(fullname);
            c.set_tag_type(expr_type, id_tag, fullname);
        }

        //System.out.println("Expr res = " + expr_res);
        //System.out.println("New dynamic type for "+ id_tag + "    " +   c.get_tag_type(id_tag));
        return null;
    }


    /*
     * f0 -> Identifier() 
     * f1 -> "[" 
     * f2 -> Expression() 
     * f3 -> "]" 
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String arr_id = n.f0.accept(this, argu);
        String index = n.f2.accept(this, argu);
        String expr = n.f5.accept(this, argu);
        String arr_addr;
        String fullname = this.st.id_lookup(argu + "::" + arr_id);
        if (!fullname.equals(argu+"::"+arr_id)) { // its a member variable
            String classname = argu.split("::")[0]; // get the classname from A::foo
            // we add 8 because, the start of an object is the pointer to the vtable
            var_t member = (var_t) st.S.get(fullname);
            int offset = 8 + this.offset_table.get_offset(fullname, this.st);
            String t = collocations_t.real_type(member.type); // store real type
            String ptr_idx = c.new_temp();
            emit(String.format("%s = getelementptr i8, i8* %s, %s %s", ptr_idx, "%this", "i32", offset));
            String casted_tag = c.new_temp(member.type);

            emit(String.format("%s = bitcast i8* %s to %s*", casted_tag, ptr_idx, t));
            arr_addr = casted_tag;
            //emit(String.format("store %s %s, %s* %s", t, expr_res, t, casted_tag));
        }
        else {
            arr_addr = c.get_tag(argu + "::" + arr_id);
        }
        //String arr_type = c.get_tag_type(arr_addr);
        String arr_type = "int";
        
        String accept_label = c.new_label("accept");
        String throw_oob = c.new_label("throw_oob");
        String exit_label = c.new_label("exit");
        int size = 4;
        String ptr = c.new_temp();

        emit("\n\n; array assignment statement");
        emit(String.format("%s = load i32*, i32** %s", ptr, arr_addr));
        String sz = c.new_temp();
        emit(String.format("%s = load i32, i32* %s", sz, ptr));
        String compare = c.new_temp();
        emit(String.format("%s = icmp ult i32 %s, %s", compare, index, sz));
        emit(String.format("br i1 %s, label %s, label %s", compare, "%"+accept_label, "%"+throw_oob));

        emit(accept_label + ":");
        String new_index = c.new_temp();
        // remember: the first element of the array is it's length, 
        // therefore we need to add 1 to get the correct index
        emit(String.format("%s = add i32 1, %s", new_index, index));
        String idx_ptr = c.new_temp();
        emit(String.format("%s = getelementptr i32, i32* %s, i32 %s", idx_ptr, ptr, new_index));
        emit(String.format("store i32 %s, i32* %s", expr, idx_ptr));
        emit(String.format("br label %s", "%"+exit_label));
        emit(throw_oob + ":");
        emit(String.format("call void @throw_oob()"));
        emit(String.format("br label %s", "%"+exit_label));
        emit(exit_label + ":");
        return null;
    }

    /*
     * f0 -> PrimaryExpression() 
     * f1 -> "[" 
     * f2 -> PrimaryExpression() 
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, String argu) throws Exception {
        //lflag = true;
        String ptr = n.f0.accept(this, argu);
        //String ptr = c.new_temp();
        String index = n.f2.accept(this, argu);

        String res = c.new_temp("int");

        String accept_label = c.new_label("accept");
        String throw_oob = c.new_label("throw_oob");
        String exit_label = c.new_label("exit");
        emit("\n; array lookup");
        int size = 4;
        //emit(String.format("%s = load i32*, i32** %s", ptr, arr_addr));
        String sz = c.new_temp();
        emit(String.format("%s = load i32, i32* %s", sz, ptr));
        String compare = c.new_temp();
        emit(String.format("%s = icmp ult i32 %s, %s", compare, index, sz));
        emit(String.format("br i1 %s, label %s, label %s", compare, "%"+accept_label, "%"+throw_oob));

        emit(accept_label + ":");
        String new_index = c.new_temp();
        // remember: the first element of the array is it's length, 
        // therefore we need to add 1 to get the correct index
        emit(String.format("%s = add i32 1, %s", new_index, index)); 
        String idx_ptr = c.new_temp();
        emit(String.format("%s = getelementptr i32, i32* %s, i32 %s", idx_ptr, ptr, new_index));
        emit(String.format("%s = load i32, i32* %s", res, idx_ptr));
        emit(String.format("br label %s", "%"+exit_label));
        emit(throw_oob + ":");
        emit(String.format("call void @throw_oob()"));
        emit(String.format("br label %s", "%" + exit_label));
        emit(exit_label + ":");;
        return res;

    }

    /*
     * f0 -> PrimaryExpression() 
     * f1 -> "." 
     * f2 -> "length"
     */
    public String visit(ArrayLength n, String argu) throws Exception {
        this.lflag = true;
        String arr_addr = n.f0.accept(this, argu);
        String ptr = c.new_temp();
        emit(String.format("%s = load i32*, i32** %s", ptr, arr_addr));
        String sz = c.new_temp("int");
        emit(String.format("%s = load i32, i32* %s", sz, ptr));
        return sz;

    }


    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }


    public String visit(PlusExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);
        String expr1_val = c.new_temp();
        String expr2_val = c.new_temp();

        String res = c.new_temp("int");
        //emit(String.format("%s = load i32, i32* %s", expr1_val, expr1));
        //emit(String.format("%s = load i32, i32* %s", expr2_val, expr2));
        emit(String.format("%s = add i32 %s, %s", res, expr1, expr2));
        return res;
    }

    public String visit(MinusExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);
        String expr1_val = c.new_temp();
        String expr2_val = c.new_temp();

        String res = c.new_temp("int");
        //emit(String.format("%s = load i32, i32* %s", expr1_val, expr1));
        //emit(String.format("%s = load i32, i32* %s", expr2_val, expr2));
        emit(String.format("%s = sub i32 %s, %s", res, expr1, expr2));
        return res;
    }

    public String visit(TimesExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);
        String expr1_val = c.new_temp();
        String expr2_val = c.new_temp();

        String res = c.new_temp("int");
        //emit(String.format("%s = load i32, i32* %s", expr1_val, expr1));
        //emit(String.format("%s = load i32, i32* %s", expr2_val, expr2));
        emit(String.format("%s = mul i32 %s, %s", res, expr1, expr2));
        return res;
    }

    /* 
     * f0 -> PrimaryExpression() 
     * f1 -> "&&" 
     * f2 -> PrimaryExpression()
     */
    public String visit(AndExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, argu);
        String res = c.new_temp("boolean");
        String res2 = c.new_temp("boolean");

        String and1 = c.new_label();
        String and2 = c.new_label();
        String and3 = c.new_label();
        String and4 = c.new_label();


        // emit(String.format("%s = or i1 0, %s", res, expr1)); // res = expr1

        // emit(String.format("br i1 %s, label %s, label %s", expr1, "%"+expr1_true, "%"+expr1_false));
        // emit(expr1_true + ":");

        // emit(String.format("%s = or i1 0, %s", res, expr2)); // res = expr2

        // emit(expr1_false + ":");

        emit(String.format("br label %s", "%" + and1));
        emit(and1 + ":");
        emit(String.format("br i1 %s, label %%%s, label %%%s", expr1,  and2, and4));
        emit(String.format(and2 + ":"));
        String expr2 = n.f2.accept(this, argu);
        emit(String.format("%s = or i1 0, %s", res2, expr2));
        emit(String.format("br label %%%s\n", and3));

        emit(and3 + ":");
        emit(String.format("br label %%%s", and4));

        emit(and4 + ":");
        emit(String.format("%s = phi i1 [0, %%%s], [%s, %%%s]", res, and1, expr2, and3));


        return res;
    }

    /*
     * f0 -> PrimaryExpression() 
     * f1 -> "<" 
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, argu);
        String expr2 = n.f2.accept(this, argu);

        String res = c.new_temp("boolean");
        emit(String.format("%s = icmp slt i32 %s, %s", res, expr1, expr2));
        return res;
    }

    /*
     * f0 -> "if" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> Statement() 
     * f5 -> "else" 
     * f6 -> Statement()
     */
    public String visit(IfStatement n, String argu) throws Exception {
        String iflabel = this.c.new_label("if");
        String elselabel = this.c.new_label("else");
        String endlabel = this.c.new_label("end");

        String expr_val = n.f2.accept(this, argu);
        emit(String.format("br i1 %s, label %s, label %s", expr_val, "%"+iflabel, "%"+elselabel));
        emit(iflabel + ":");
        n.f4.accept(this, argu);
        emit(String.format("br label %s", "%"+endlabel));
        emit(elselabel + ":");
        n.f6.accept(this, argu);

        // Remember, every Label declaration must have br operation before
        emit(String.format("br label %s", "%"+endlabel));  // useless
        emit(endlabel +":");
        return null;
    }

    /*
     * f0 -> "while" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, String argu) throws Exception {
        String start_label = c.new_label("start");
        String do_label = c.new_label("do");
        String exit_label = c.new_label("exit");

        emit(String.format("br label %s", "%"+start_label)); //useless br (just to prevent error)
        emit(start_label + ":");
        String expr_val = n.f2.accept(this, argu);
        emit(String.format("br i1 %s, label %s, label %s", expr_val, "%"+do_label, "%"+exit_label));
        emit(do_label+":");
        n.f4.accept(this, argu);
        emit(String.format("br label %s", "%"+start_label)); // jump back
        emit(exit_label +":");

        return null;
    }


    /*
     * f0 -> "new" 
     * f1 -> Identifier() 
     * f2 -> "(" 
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, String argu) throws Exception {
        return this.allocate_object(n.f1.accept(this, argu)); // an identifier of the result will be returned
    }

    /*
     * f0 -> "new" 
     * f1 -> "int" 
     * f2 -> "[" 
     * f3 -> Expression() 
     * f4 -> "]"
     */
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        String expr_val = n.f3.accept(this, argu);

        String cmp_res = c.new_temp();
        String allocated_mem = c.new_temp();
        String ptr2int = c.new_temp("int[]");
        String is_out_of_bounds = c.new_label("out_of_bounds");
        String array_alloc =  c.new_label("array_alloc");

        emit("\n; array allocation expression");
        emit(String.format("%s = icmp slt i32 %s, 0", cmp_res, expr_val)); // if expr < 0
        emit(String.format("br i1 %s, label %s, label %s", cmp_res, "%"+is_out_of_bounds, "%"+array_alloc)); // print "out of bounds" runtime error
        emit(is_out_of_bounds + ":");
        emit("call void @throw_oob()");
        emit(String.format("br label %s", "%" + array_alloc));
        emit(array_alloc + ":");
        String new_sz = c.new_temp();
        emit(String.format("%s = add i32 1, %s", new_sz, expr_val)); // size = 1 + expression    because *(ARRAY + 0) is the length of the array
        emit(String.format("%s = call i8* @calloc(i32 4, i32 %s)", allocated_mem, expr_val));
        emit(String.format("%s = bitcast i8* %s to i32*", ptr2int, allocated_mem));
        emit(String.format("store i32 %s, i32* %s", expr_val, ptr2int)); // store the length to the first element of the array

        //
        // String res = c.new_temp("int[]");
        // emit(String.format("%s = bitcast i32* %s to i32**", res, ptr2int));

        return ptr2int;

    }
    


    /*
     * f0 -> "System.out.println" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> ";"
     */
    public String visit(PrintStatement n, String argu) throws Exception {
        String expr_res = n.f2.accept(this, argu); // returns the identifier holding the expr result
        //String l = c.new_temp();
        //emit(String.format("%s = load i32, i32* %s", l, expr_res)); 
        emit(String.format("call void @print_int(i32 %s)\n", expr_res));
        return null;
    }
    /*
     * f0 -> PrimaryExpression() 
     * f1 -> "." 
     * f2 -> Identifier() 
     * f3 -> "(" 
     * f4 -> ( ExpressionList() )? 
     * f5 -> ")" 
     */ 
    public String visit(MessageSend n, String argu) throws Exception {
        //this.lflag = true; // we need object tag to be an address
        String obj_tag = n.f0.accept(this, argu); // tag is holding the object
        String fun_id = n.f2.accept(this, argu) + "__";
        String obj_type = c.get_tag_type(obj_tag);

        // get the correct function fullname, using the vtable_t type implemented
        String fun_fullname = ((class_t)st.S.get(obj_type)).vtable.vt.get(fun_id);
        int fun_offset = offset_table.get_offset(fun_fullname, this.st); // get the location of the function

        String obj_ptr = c.new_temp();
        String casted_ptr = c.new_temp();
        String vptr = c.new_temp();
        String fun_ptr = c.new_temp();
        String fun_addr = c.new_temp();
        String casted_fun_addr = c.new_temp();
        emit("\n; method call");
        //emit(String.format("%s = load i8*, i8** %s", obj_ptr, obj_tag));
        //emit(String.format("%s = bitcast i8* %s to i8***", casted_ptr, obj_ptr));


        emit(String.format("%s = bitcast i8* %s to i8***", casted_ptr, obj_tag));
        emit(String.format("%s = load i8**, i8*** %s", vptr, casted_ptr));
        emit(String.format("%s = getelementptr i8*, i8** %s, i32 %d", fun_ptr, vptr, fun_offset/8));
        emit(String.format("%s = load i8*, i8** %s", fun_addr, fun_ptr));


        method_t m = (method_t) st.S.get(fun_fullname);
        String ret = c.new_temp(m.ret_type);
        //String ll_fun_name = new String(fun_fullname);

        String[] expr_arr = n.f4.present() ? n.f4.accept(this, argu).split(",") : null;


        String expr_list = new String("");
        String ll_function_type = new String("");
        ll_function_type += collocations_t.real_type(m.ret_type) + " (i8*";
        expr_list += "i8* " + obj_tag;
        for (int i = 0; i < m.paramlist_types.size(); i++) {
            String conv_type = collocations_t.real_type(m.paramlist_types.get(i));
            expr_list += "," + conv_type + " " + expr_arr[i];
            ll_function_type += "," + conv_type;
        }
        ll_function_type += ")*";
        emit(String.format("%s = bitcast i8* %s to %s", casted_fun_addr, fun_addr, ll_function_type));
        emit(String.format("%s = call %s %s(%s)", ret, collocations_t.real_type(m.ret_type)
                                                 , casted_fun_addr, expr_list));

        return ret;
    }
    /* 
     * f0 -> IntegerLiteral() 
     * | TrueLiteral() 
     * | FalseLiteral() 
     * | Identifier() 
     * | ThisExpression() 
     * | ArrayAllocationExpression() 
     * | AllocationExpression() 
     * | NotExpression() 
     * | BracketExpression()
     */
    public String visit(PrimaryExpression n, String argu) throws Exception {
        String expr = n.f0.accept(this, argu);
        if (expr.equals("true") || expr.equals("false")) {
            String bool_tag = c.new_temp("boolean");
            emit(String.format("%s = or i1 0, %s", bool_tag, expr.equals("true") ? "1" : "0"));
            return bool_tag;
        }
        if (expr.equals("this")) {
            String scope = new String(argu);
            return c.new_temp_this_tag(scope.split("::")[0]);
        }
        //if (isnumber(expr)) return expr; // if is IntegerLiteral(), return it
        if (expr.charAt(0) == '%') return expr; // tag already resolved


        // in case its an identifier
        String fullname = st.id_lookup(argu + "::" + expr);
        var_t v = (var_t) st.S.get(fullname);

        if (lflag) { // if we lvalue is asked, then return the address
            lflag = false; // re-init the flag
            if (!fullname.equals(argu+"::"+expr)) { // if different, then Identifier() is a member variable
                String classname = argu.split("::")[0]; // get the classname from A::foo
                // we add 8 because, the start of an object is the pointer to the vtable
                var_t member = (var_t) st.S.get(fullname);
                int offset = 8 + this.offset_table.get_offset(fullname, this.st);

                String t = collocations_t.real_type(member.type); // store real type
                String ptr_idx = c.new_temp();
                emit(String.format("%s = getelementptr i8, i8* %s, %s %s", ptr_idx, "%this", "i32", offset));
                String casted_tag = c.new_temp(member.type);
                emit(String.format("%s = bitcast i8* %s to %s*", casted_tag, ptr_idx, t));
                //String id_val = c.new_temp(member.type);
                //emit(String.format("%s = load %s, %s* %s", id_val, t, t, casted_tag));
                return casted_tag;
            }
            return c.get_tag(argu + "::" + expr);
        }
        else { // return the value of the Identifier()

            if (!fullname.equals(argu+"::"+expr)) { // if different, then Identifier() is a member variable
                String classname = argu.split("::")[0]; // get the classname from A::foo
                // we add 8 because, the start of an object is the pointer to the vtable
                var_t member = (var_t) st.S.get(fullname);
                int offset = 8 + this.offset_table.get_offset(fullname, this.st);

                String t = collocations_t.real_type(member.type); // store real type
                String ptr_idx = c.new_temp();
                emit(String.format("%s = getelementptr i8, i8* %s, %s %s", ptr_idx, "%this", "i32", offset));
                String casted_tag = c.new_temp(member.type);
                emit(String.format("%s = bitcast i8* %s to %s*", casted_tag, ptr_idx, t));
                String id_val;
                if (isprimitive(member.type))
                    id_val = c.new_temp(member.type);
                else
                    id_val = c.new_temp(member.dynamic_type);
                emit(String.format("%s = load %s, %s* %s", id_val, t, t, casted_tag));
                return id_val;
            }
            else {
                String idaddr = c.get_tag(fullname);
                String id_val;
                if (isprimitive(v.type))
                    id_val = c.new_temp(v.type);
                else if (v.dynamic_type != null) {
                    id_val = c.new_temp(v.dynamic_type);
                }
                else
                    id_val = c.new_temp(v.type);
                String idtype = collocations_t.real_type(v.type);
                emit(String.format("%s = load %s, %s* %s", id_val, idtype, idtype, idaddr));
                return id_val;
            }
        }
            

    }


    /*
     * f0 -> "!" 
     * f1 -> PrimaryExpression()
     */
    public String visit(NotExpression n, String argu) throws Exception {
        String primexpr = n.f1.accept(this, argu);
        String res = c.new_temp("boolean");
        emit(String.format("%s = xor i1 1, %s", res, primexpr));

        return res;

    }

    /*
     * f0 -> "(" 
     * f1 -> Expression() 
     * f2 -> ")"
     */
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
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
        String tag = c.new_tag(type, argu + "::" + name);
        // add a comment as well
        emit(tag + " = alloca " + collocations_t.real_type(type) + "   ; " + type + " " + name);
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
        for (Node node: n.f0.nodes) {
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
        //String id = c.new_tag(type, name);
        return type + " " + name;
    }


    public String visit(ArrayType n, String argu) { return "int[]"; }

    public String visit(BooleanType n, String argu) { return "boolean"; }

    public String visit(IntegerType n, String argu) { return "int"; }

    public String visit(Identifier n, String argu) { return n.f0.toString(); }

    public String visit(FalseLiteral n, String argu) {  return "false";  }
    public String visit(TrueLiteral n, String argu) { return "true"; }
    public String visit(ThisExpression n, String argu) { return "this"; }

    public String visit(IntegerLiteral n, String argu) throws Exception { 
        String literal = n.f0.toString();
        String res = c.new_temp("int"); 
        // emit(String.format("%s = alloca i32", res));
        // emit(String.format("store i32 %s, i32* %s", literal, res));
        emit(String.format("%s = or i32 %s, %s", res, "0",literal)); // return the value of the literal
        return res;
    }

    public static boolean isnumber(String str) { 
        try {  
            Double.parseDouble(str);  
            return true;
        } catch(NumberFormatException e){  
            return false;  
        }   
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