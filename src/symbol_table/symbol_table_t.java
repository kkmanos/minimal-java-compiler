package symbol_table;
import java.io.IOException;
import java.util.LinkedHashMap;

import java.io.*;

public class symbol_table_t {
    // key: fullname, value:details of this fullname (name_t)
    // LinkedHashMap, because it is ordered by the key inserted
    // thus, being usefile while printing or traversing for offset.
    public LinkedHashMap<String, name_t> S = new LinkedHashMap<String, name_t>();
    // fullname is in format "A::foo" for a foo function in class A
    // or "A" for just a class
    // or "" for global scope


    public int get_sizeof_struct(String classname) {
        // pass the symboltable, to let the get_sizeof_struct recurse
        // to it's superclasses
        return ((class_t)S.get(classname)).get_sizeof_struct(this);
    }
    
    // returns the fullname of the method
    public String vtable_lookup(String classname, String method_id) {
        class_t cl = (class_t) S.get(classname);
        if (cl == null) return null;
        String m = cl.vtable.vt.get(method_id);
        return m; // if not found, will return null
    }


    public boolean insert_var(String scope, String id, String type) throws Exception {
        String fullname = new String(scope + "::" + id);

        if (S.get(fullname) != null) // if already exists
            return false; // failed
        var_t var = new var_t(id, type);
        S.put(fullname, var);
        if (S.get(scope).get_type().equals("class_t")) {
            class_t cl = (class_t) S.get(scope);
            cl.member_vars.add(var);
        }
        return true;
    }

    public boolean insert_method(String classname, String id, String ret_type) throws Exception {
        String fullname = new String(classname + "::" + id);


        if (S.get(fullname) != null) { // if already exists
            return false; // failed
        }

        name_t method = new method_t(id, ret_type);
        class_t cl = (class_t) S.get(classname);

        cl.vtable.vt.put(id, fullname); // insert { id , fullname } into vtable of classname
        S.put(fullname, method);
        return true;
    }


    public boolean insert_param_type(String fullname, String param_type) {
        name_t meth = S.get(fullname);
        if (!meth.get_type().equals("method_t"))
            return false;
        
        ((method_t)meth).insert_param_type(param_type);
        return true;
    }


    public boolean insert_class(String classname, String extends_class) throws Exception {
        if (S.get(classname) != null) { // if already exists
            return false;
        }
        class_t cl = new class_t(classname, extends_class);
        if (extends_class != null) {
            class_t supercl = (class_t) S.get(extends_class);
            cl.vtable.copy(supercl.vtable);
        }
        S.put(new String(classname), cl);
        return true;
    }


    // usefull for variables only

    public String lookup(String fullname) { // search for variables or functions or classes

        
        if (fullname.contains("::")) { // this is not a class
            String[] s = fullname.split("::");
            //System.out.println(get_next_scope(fullname));
            return rec_lookup(get_next_scope(fullname), s[s.length-1]);
        }
        else { // this is a classname (beacause its on global scope)
            if (S.get(fullname) != null)
                return fullname; // return the classname
            else return null;
        }
    }

    public String rec_lookup(String scope, String key) {
        if (scope.equals("")) { 
              for (String classname : S.keySet())
                   if (!classname.contains("::") && key.equals(classname))
                       return classname;
            return null;
        }

        String entry = new String(scope + "::" + key);
        // if entry exists in symboltable
        //if (S.get(entry) != null && (type == null || S.get(entry).get_type().equals(type))) { 
        if (S.get(entry) != null) {
             if (S.get(entry).get_type().equals("var_t"))
                 return ((var_t)S.get(entry)).type;
        }
        // if scope is a class, then check if it has a superclass. If we look at superclass
        // we will only look for variables and not methods.
        if (S.get(scope) != null && S.get(scope).get_type().equals("class_t") && ((class_t)S.get(scope)).supername != null) {
            String supname = ((class_t)S.get(scope)).supername;
            String t;
            // check in supername's scope for the key
            //if ( (t = rec_lookup(supname, key, type)) != null && S.get(supname + "::"+key) != null && !S.get(supname + "::"+key).get_type().equals("method_t")) // check before returning
            if ((t = rec_lookup(supname, key)) != null)
                return t;   // because it could make the last return useless
        }
        return rec_lookup(get_next_scope(scope),key);
    }

    public String get_next_scope(String scope) {

        if (scope.equals("") || !scope.contains("::"))
            return "";
        String[] s = scope.split("::");
        String new_scope = "";
        for (int i = 0; i < s.length-1; i++) {
            if (i == 0)
                new_scope = new_scope + s[i];
            else
                new_scope = new_scope + "::" + s[i];
        }
        return new_scope;
    }

    public boolean is_method(String fullname) {
        if (S.get(fullname) == null) return false;
        return (S.get(fullname).get_type().equals("method_t"));
    }

    public method_t get_method(String fullname) {
        if (is_method(fullname))
            return (method_t) S.get(fullname);
        return null;
    }

    public void print() {
        for (String fullname : S.keySet()) {
            System.out.println(fullname);
            S.get(fullname).print();
        }
    }

    public String get_parent_class(String classname) {
        class_t cl = (class_t)S.get(classname);
        if (cl == null) return null; // if does not exist, return null
        return (cl.supername != null) ? cl.supername : null;
    }



    // similar with lookup, but in this function returns the identifier found
    /// else returns null
    public String id_lookup(String fullname) { // search for variables or functions or classes
        if (fullname.contains("::")) { // this is not a class
            String[] s = fullname.split("::");
            //System.out.println(get_next_scope(fullname));
            return rec_id_lookup(get_next_scope(fullname), s[s.length-1]);
        }
        else { // this is a classname (beacause its on global scope)
            if (S.get(fullname) != null)
                return fullname; // return the classname
            else return null;
        }
    }

    public String rec_id_lookup(String scope, String key) {
        if (scope.equals("")) { 
            return null;
        }

        String entry = scope + "::" + key;
        // if entry exists in symboltable
        if (S.get(entry) != null) { 
            return entry;
        }
        // if scope is a class, then check if it has a superclass. If we look at superclass
        // we will only look for variables and not methods.
        if (S.get(scope) != null && S.get(scope).get_type().equals("class_t") && ((class_t)S.get(scope)).supername != null) {
            String supname = ((class_t)S.get(scope)).supername;
            String t;
            // check in supername's scope for the key
            //if ( (t = rec_id_lookup(supname, key)) != null && S.get(supname + "::"+key) != null && !S.get(supname + "::"+key).get_type().equals("method_t")) // check before returning
            if ( (t = rec_id_lookup(supname, key)) != null)
                return t;   // because it could make the last return useless
        }
        return rec_id_lookup(get_next_scope(scope),key);
    }


}

