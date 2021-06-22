package llvm_structs;
import java.util.LinkedHashMap;

import java.io.*;
import java.util.LinkedHashMap;
import symbol_table.*;
public class collocations_t {
    // Store all collocations in format: namemap= {key: fullname, val: tuple_t*} 
    // fullname is in format: "A::foo:i" 
    LinkedHashMap<String, tuple_t> namemap = new LinkedHashMap<>();

    // tagmap= {key: tag, val: tuple_t*}
    LinkedHashMap<String, tuple_t> tagmap = new LinkedHashMap<>(); 
    symbol_table_t st = null;
    int counter;
    int label_count;
    public collocations_t(symbol_table_t ST) {
        this.counter = 0;
        this.st = ST;
        this.label_count = 0;
    }
    public String new_tag(String type, String fullname) {
        String tag = new String("%_" + fullname.replace("::", "."));
        namemap.put(fullname, new tuple_t(type, tag));
        tagmap.put(tag, new tuple_t(type, tag));
        return tag;
    }

    public String new_fun(String fullname) {
        String tag = new String("@"+fullname.replace("::", "."));
        String type = ((method_t) st.S.get(fullname)).ret_type;
        namemap.put(fullname, new tuple_t(type, tag)); // type for function tag is its return type
        return tag;
    }

    public String new_temp(String type) { 
        String tag = new String( "%_" + counter);
        counter++;
        tagmap.put(tag, new tuple_t(type, tag));
        return tag;
    }

    public String new_temp() { // when we dont need to insert it
        String tag = new String("%_" + counter);
        counter++;
        return tag;
    }

    public String get_tag(String fullname) {
        return (namemap.get(fullname) != null) ? namemap.get(fullname).tag : null;
    }

    public String get_tag_type(String tag) {
        return tagmap.get(tag).type;
    } 

    public String new_temp_this_tag(String type) {
        tagmap.remove("%this");
        tagmap.put("%this", new tuple_t(type, "%this"));
        return "%this";
    }

    public void set_tag_type(String type, String tag, String fullname) {
        tuple_t t = namemap.get(fullname);
        t.type = new String(type);
        tuple_t t2 = tagmap.get(tag);
        t2.type = new String(type);
    }




    public static String real_type(String t) {
        if (t.equals("int")) return "i32";
        if (t.equals("int[]")) return "i32*";
        if (t.equals("boolean")) return "i1";
        return "i8*"; // in case its a pointer
    }

    public void write_class_vtable(String classname, Writer w) throws Exception {
        class_t cl = (class_t) st.S.get(classname);

        // write the size of the vtable
        w.write("@."+classname+"_vtable = global ["+cl.vtable.vt.size()+" x i8*] [");

        boolean writen_first = false; // will be set to true if the first method is writen 

        // write all method prototypes
        for (String methodid : cl.vtable.vt.keySet()) {
            String method_fullname = cl.vtable.vt.get(methodid);
            method_t m = (method_t) st.S.get(method_fullname);

            // if this is not the first, then add  a comma
            if (writen_first == true) w.write(", ");

            w.write("i8* bitcast (" + real_type(m.ret_type) + " (i8*");
            for (int i = 0; i < m.paramlist_types.size(); i++) {
                w.write(", "+real_type(m.paramlist_types.get(i)));
            }
            if (writen_first == false) writen_first = true;

            w.write(")* @"+method_fullname.replace("::", ".") + " to i8*)");


        }
        System.out.println("\n\n\n");
        w.write("]\n");
    }


    public static int get_sizeof(String type) { // implementation of C sizeof()
        if (type.equals("int")) return 4;
        if (type.equals("boolean")) return 1;
        return 8;
    }

    public String new_label() { 
        // arg can be "if", "else" or anything, and is needed, cause labels start with non-numerics
        return new String("L_" + this.label_count++); 
    }

    public String new_label(String name) {
        return new String("L_" + name + "_" + this.label_count++);
    }


}
