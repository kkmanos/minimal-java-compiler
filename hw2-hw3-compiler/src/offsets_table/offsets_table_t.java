package offsets_table;
import java.util.LinkedHashMap;
import symbol_table.*;
public class offsets_table_t {
    // {key: name, value: offset}    name can be A.foo
    LinkedHashMap<String, Integer> offtable = new LinkedHashMap<>();


    // returns the offset of fullname. It is recursive.
    // because overrided members of a class are not re-written in
    // in the offtable as a new record. 
    public int get_offset(String fullname, symbol_table_t st) {
      //  if (st.S.get(fullname).get_type().equals("var_t")) {
            Integer off = offtable.get(fullname.replace("::", "."));
            if (off != null) return off;
             // get supername of the  fullname's class
            String supername = ((class_t)st.S.get(fullname.split("::")[0])).supername; 
            String super_entry = supername + "::" + fullname.split("::")[1]; // get method id from fullname
            off = offtable.get(super_entry.replace("::", "."));
            if (off != null) 
                return off;
            else
                return get_offset(super_entry, st);
        // }
        // else {
        //     Integer off = offtable.get(fullname.replace("::", "."));
        //     if (off != null) return off;
        //     // get supername of the  fullname's class
        //     String supername = ((class_t)st.S.get(fullname.split("::")[0])).supername; 
        //     String super_entry = supername + "::" + fullname.split("::")[1]; // get method id from fullname
        //     off = offtable.get(super_entry.replace("::", "."));
        //     if (off != null) 
        //         return off;
        //     else
        //         return get_offset(super_entry, st);
        // }
    }

    public void init_and_print(symbol_table_t st) {
        int method_offset = 0;
        int var_offset = 0;
        System.out.println("== PRINTING OFFSET TABLE ==");
        for (String fn : st.S.keySet()) { // for each fullname in symbol table
            name_t n = st.S.get(fn);
            String s = new String(fn);
            String classname;
            String member;
            if (!s.contains("::"))
                classname = new String(s);
            else {
                String[] parts = s.split("::");
                if (parts.length > 2)
                    continue;
                classname = parts[0];
                member = parts[1];
            }

            if (n.get_type().equals("class_t")) { // if fn is a class
                class_t cl = (class_t) n;
                if (cl.supername == null) // if there is no superclass, then restart the counters
                    var_offset = method_offset = 0;
                System.out.println("-------- Class "+cl.id+" --------");
                continue;
            }

            if (n.get_type().equals("var_t")) {
                var_t v = (var_t) n;

                String toprint = fn.replace("::", ".");
                System.out.println(toprint + " : " + var_offset);
                this.offtable.put(toprint, var_offset);
                if (v.type.equals("int"))
                    var_offset += 4;
                else if (v.type.equals("boolean"))
                    var_offset += 1;
                else // if its another type , int[] or class, then its 8bytes (address)
                    var_offset += 8;
            }

            if (n.get_type().equals("method_t")) {
                method_t m = (method_t) n;

                String toprint = fn.replace("::", ".");
                String supername = ((class_t)st.S.get(classname)).supername;
                if (supername == null) {

                    System.out.println(toprint + " : " + method_offset);
                    this.offtable.put(toprint, method_offset);
                    method_offset += 8;
                    continue;
                }
                method_t msup = (method_t) st.S.get(supername+"::"+m.id);
                if ( msup != null && msup.same_prototype(m)) { // if m method is an override of msup
                   continue;        // then dont increase the offsert counter 
                }
                else {
                    System.out.println(toprint + " : " + method_offset);
                    this.offtable.put(toprint, method_offset);
                    method_offset += 8;
                }

            }

        }
    }
}
