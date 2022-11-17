package symbol_table;

import java.util.LinkedHashMap;

public class vtable_t {
	
	// { key : method_id, value: fullname to the method this record points to}
	public LinkedHashMap<String, String> vt = new LinkedHashMap<>();

	public void copy(vtable_t vtable2) { // copy the vtable
	   for (String key : vtable2.vt.keySet()) {
		   this.vt.put(key, vtable2.vt.get(key));
		}
	}

	public void print() {
		System.out.println("--VT PRINT--");
		for (String key : this.vt.keySet()) {
			System.out.println(key + " -- " + this.vt.get(key));
		}
		System.out.println();
	}



}
