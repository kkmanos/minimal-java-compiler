package llvm_structs;
import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

import llvm_structs.collocations_t;
import offsets_table.offsets_table_t;
import semantic_analysis.*;
import symbol_table.*;
import java.io.*;

public class llvm_vtable_visitor extends GJDepthFirst <String,String> {

    public Writer writer = null;
    public offsets_table_t offset_table = null;
    public symbol_table_t st = null;
    public collocations_t c = null;
    public llvm_vtable_visitor(Writer w, offsets_table_t offt, symbol_table_t ST) throws Exception {
        writer = w;
        offset_table = offt;
        st = ST;
        c = new collocations_t(st);
    }

    public void emit(String to_emit) throws Exception {
        this.writer.write(to_emit + "\n");
    }
    /**
        * f0 -> MainClass()
        * f1 -> ( TypeDeclaration() )*
        * f2 -> <EOF>
        */
    public String visit(Goal n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        emit("\n\ndeclare i8* @calloc(i32, i32)");
        emit("declare i32 @printf(i8*, ...)");
        emit("declare void @exit(i32)");
        emit("@_cint = constant [4 x i8] c\"%d\\0a\\00\"");
        emit("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"");
        emit("define void @print_int(i32 %i) {");
        emit("  %_str = bitcast [4 x i8]* @_cint to i8*");
        emit("  call i32 (i8*, ...) @printf(i8* %_str, i32 %i)");
        emit("  ret void\n" + "}");
        emit("define void @throw_oob() {");
        emit("  %_str = bitcast [15 x i8]* @_cOOB to i8*");
        emit("  call i32 (i8*, ...) @printf(i8* %_str)");
        emit("  call void @exit(i32 1)");
        emit("  ret void\n}\n\n");
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
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String _ret=null;
        n.f0.accept(this, argu);
        String classname = n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, classname);
        c.write_class_vtable(classname, this.writer);
        n.f5.accept(this, argu);
        return _ret;
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
   public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
      String _ret=null;
      n.f0.accept(this, argu);
      String classname = n.f1.accept(this, argu);
      n.f2.accept(this, argu);
      String supername = n.f3.accept(this, argu);
      n.f4.accept(this, argu);
      n.f5.accept(this, argu);
      n.f6.accept(this, classname);
      c.write_class_vtable(classname, this.writer);
      n.f7.accept(this, argu);
      return _ret;
   }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, String argu) throws Exception {
        return n.f0.toString();
    }


}