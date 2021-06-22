package symbol_table;

import java.util.ArrayList;

public class method_t extends name_t {
    //String memberof;
    public String ret_type;
    //HashMap<String, symbol_table.var_t> locals = new HashMap<String, String>();
    public ArrayList<String> paramlist_types = new ArrayList<String>(); // array of types

    public method_t(String method_name, String ret_type) {
        super(method_name);
        this.ret_type = ret_type;
    }

    public void insert_param_type(String type) {
        paramlist_types.add(type);
    }

    public boolean same_prototype(method_t m2) {
        // if the number of parameters differ
        if (m2.paramlist_types.size() != this.paramlist_types.size())
            return false;
        // if there is at least one different parameter type
        for (int i = 0; i < this.paramlist_types.size(); i++) {
            if (!this.paramlist_types.get(i).equals(m2.paramlist_types.get(i)))
                return false;
        }
        // if return types differ
        if (!this.ret_type.equals(m2.ret_type))
            return false;
        return true;
    }

    public void print() {

        String f = String.format("\t\t(METHOD) name: %s, ret_type: %s", id, ret_type);
        System.out.println(f);
        String f2 = String.format("\t\tparameter list types: %s", paramlist_types.toString());
        System.out.println(f2);
    }

    public String get_type() { return "method_t"; }
}
