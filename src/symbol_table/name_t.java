package symbol_table;

public abstract class name_t {
    public String id; // just an identifier name, not the full name

    public name_t(String identifier) {
        this.id = identifier;
    }

    public void print() {
        System.out.println("\t\t(ID) id: " + this.id);
    }
    public abstract String get_type();
}
