package symbol_table;

public class var_t extends name_t {
    public String type;

    // Type of the object this variable is pointing to. (only in case this variable is pointing to an object)
    public String dynamic_type;

    public var_t(String id, String type) {
        super(id);
        this.type = new String(type);
        this.dynamic_type = null;  

    }

    public void set_dynamic_type(String dyn) { this.dynamic_type =new String( dyn); }

    public void print() {

        String f = String.format("\t\t(VARIABLE) name: %s, type: %s", id, type);
        if (dynamic_type != null)
            f += "\n\t\tdynamic_type: " + dynamic_type;
        System.out.println(f);
    }

    public String get_type() { return "var_t"; }
}
