package symbol_table;
import llvm_structs.*;

import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class class_t extends name_t {

    public String supername = null;
    public vtable_t vtable = new vtable_t();
    public ArrayList<var_t> member_vars = new ArrayList<>(); // fullnames of the member variables

    public class_t(String classname, String extends_class) {
        super(classname);
        this.supername = extends_class;
    }
    public void print() {

        String f = String.format("\t\t(CLASS) name: %s", this.id);
            
            
        
        System.out.println(f);
        if (supername != null) {
            String f2 = String.format("\t\tsuperclass: %s", this.supername);
            System.out.println(f2);
        }

        vtable.print();

        // for (int i = 0; i < var_fullnames.size(); i++)
        //     System.out.println(var_fullnames.get(i));
    }

    public int get_sizeof_struct(symbol_table_t st) {
        int sz = 0;
        for (int i = 0; i < member_vars.size(); i++) {
            sz +=  collocations_t.get_sizeof(member_vars.get(i).type);
        }

        if (supername != null) { // recursively ,get the size of the whole struct
            sz += ((class_t)st.S.get(supername)).get_sizeof_struct(st);
        }
        return sz;
    }

    public String get_type() { return "class_t"; }
}
