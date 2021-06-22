package llvm_structs;
public class tuple_t {
    public String type = null;
    public String tag = null; 

    public tuple_t(String _type, String _tag) {
        if (_type != null)
            type = new String(_type);
        if (_tag != null)
            tag = new String(_tag);
    }
}