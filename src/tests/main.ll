@.A_vtable = global [1 x i8*] [i8* bitcast (i32 (i8*)* @A.foo__ to i8*)]
@.B_vtable = global [3 x i8*] [i8* bitcast (i32 (i8*)* @A.foo__ to i8*), i8* bitcast (i1 (i8*)* @B.bla__ to i8*), i8* bitcast (i32 (i8*)* @B.printAll__ to i8*)]


declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)
@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"
define void @print_int(i32 %i) {
  %_str = bitcast [4 x i8]* @_cint to i8*
  call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
  ret void
}
define void @throw_oob() {
  %_str = bitcast [15 x i8]* @_cOOB to i8*
  call i32 (i8*, ...) @printf(i8* %_str)
  call void @exit(i32 1)
  ret void
}





define i32 @main() {
%_Main.main__.beta = alloca i8*   ; A beta


; assignment statement follows

; Allocating object
%_0 = call i8* @calloc(i32 1, i32 29)
%_1 = bitcast i8* %_0 to i8***
%_2 = getelementptr [3 x i8*], [3 x i8*]* @.B_vtable, i32 0, i32 0
store i8** %_2, i8*** %_1

store i8* %_0, i8** %_Main.main__.beta
%_3 = load i8*, i8** %_Main.main__.beta

; method call
%_5 = bitcast i8* %_3 to i8***
%_6 = load i8**, i8*** %_5
%_7 = getelementptr i8*, i8** %_6, i32 0
%_8 = load i8*, i8** %_7
%_9 = bitcast i8* %_8 to i32 (i8*)*
%_10 = call i32 %_9(i8* %_3)
call void @print_int(i32 %_10)

%_11 = load i8*, i8** %_Main.main__.beta

; method call
%_13 = bitcast i8* %_11 to i8***
%_14 = load i8**, i8*** %_13
%_15 = getelementptr i8*, i8** %_14, i32 2
%_16 = load i8*, i8** %_15
%_17 = bitcast i8* %_16 to i32 (i8*)*
%_18 = call i32 %_17(i8* %_11)
call void @print_int(i32 %_18)

%_19 = or i32 0, 1
%_20 = or i32 0, 2
%_21 = icmp slt i32 %_19, %_20
br i1 %_21, label %L_if_0, label %L_else_1
L_if_0:
%_22 = or i32 0, 500
call void @print_int(i32 %_22)

br label %L_end_2
L_else_1:
%_23 = or i32 0, 600
call void @print_int(i32 %_23)

br label %L_end_2
L_end_2:
ret i32 0
}
define i32 @A.foo__(i8* %this) {


; assignment statement follows
%_24 = or i32 0, 200
%_25 = getelementptr i8, i8* %this, i32 8
%_26 = bitcast i8* %_25 to i32*
store i32 %_24, i32* %_26


; assignment statement follows
%_27 = or i1 0, 0
%_28 = getelementptr i8, i8* %this, i32 12
%_29 = bitcast i8* %_28 to i1*
store i1 %_27, i1* %_29


; assignment statement follows
%_30 = or i32 0, 300
%_31 = getelementptr i8, i8* %this, i32 13
%_32 = bitcast i8* %_31 to i32*
store i32 %_30, i32* %_32
%_33 = or i32 0, 1
ret i32 %_33
}



define i1 @B.bla__(i8* %this) {
%_34 = or i1 0, 1
ret i1 %_34
}



define i32 @B.printAll__(i8* %this) {
%_35 = getelementptr i8, i8* %this, i32 8
%_36 = bitcast i8* %_35 to i32*
%_37 = load i32, i32* %_36
call void @print_int(i32 %_37)

%_38 = getelementptr i8, i8* %this, i32 13
%_39 = bitcast i8* %_38 to i32*
%_40 = load i32, i32* %_39
call void @print_int(i32 %_40)

%_41 = or i32 0, 1
ret i32 %_41
}



