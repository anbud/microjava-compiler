/*
 *  MicroJava Compiler 0.0.1
 * 
 *  Copyright (C) 2016 - Andrej Budinčević
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package rs.zx.mj;

public class Tab {
	public static Scope curScope;	
	public static int   curLevel;

	public static Struct intType;	
	public static Struct charType;
	public static Struct nullType;
	public static Struct noType;
	public static Obj chrObj;	
	public static Obj ordObj;
	public static Obj lenObj;
	public static Obj noObj;
	
	private static void error(String msg) {
		Errors.println(Scanner.line, Scanner.col, msg);
	}

	public static void openScope() {
		Scope s = new Scope();
		s.outer = curScope;
		curScope = s;
		curLevel++;
	}

	public static void closeScope() {
		curScope = curScope.outer;
		curLevel--;
	}

	// Create a new object with the given kind, name and type
	// and insert it into the top scope.
	public static Obj insert(int kind, String name, Struct type) {
		Obj ret = new Obj(kind, name, type);
		
		if(ret.kind == Obj.Var) {
			ret.adr = curScope.nVars++;
			ret.level = curLevel;
		}
		
		Obj iter = curScope.locals;
		Obj last = null;
		
		for(; iter != null; last = iter, iter = iter.next) 
			if(iter.name.equals(ret.name))
				error(ret.name + " already defined.");
		
		if(last == null)
			curScope.locals = ret;
		else
			last.next = ret;
		
		return ret;
			
	}

	// Retrieve the object with the given name from the top scope
	public static Obj find(String name) {
		for(Scope s = curScope; s != null; s = s.outer)
			for(Obj o = s.locals; o != null; o = o.next)
				if(o.name.equals(name))
					return o;
		
		error(name + " undefined.");
		return noObj;
	}

	// Retrieve a class field with the given name from the fields of "type"
	public static Obj findField(String name, Struct type) {
		for(Obj o = type.fields; o != null; o = o.next)
			if(o.name.equals(name))
				return o;
		
		error(name + " undefined.");
		return noObj;
	}

	public static void dumpStruct(Struct type) {
		String kind;
		switch (type.kind) {
			case Struct.Int:  kind = "Int  "; break;
			case Struct.Char: kind = "Char "; break;
			case Struct.Arr:  kind = "Arr  "; break;
			case Struct.Class:kind = "Class"; break;
			default: kind = "None";
		}
		System.out.print(kind+" ");
		if (type.kind == Struct.Arr) {
			System.out.print(type.nFields + " (");
			dumpStruct(type.elemType);
			System.out.print(")");
		}
		if (type.kind == Struct.Class) {
			System.out.println(type.nFields + "<<");
			for (Obj o = type.fields; o != null; o = o.next) dumpObj(o);
			System.out.print(">>");
		}
	}

	public static void dumpObj(Obj o) {
		String kind;
		switch (o.kind) {
			case Obj.Con:  kind = "Con "; break;
			case Obj.Var:  kind = "Var "; break;
			case Obj.Type: kind = "Type"; break;
			case Obj.Meth: kind = "Meth"; break;
			default: kind = "None";
		}
		System.out.print(kind+" "+o.name+" "+o.val+" "+o.adr+" "+o.level+" "+o.nPars+" (");
		dumpStruct(o.type);
		System.out.println(")");
	}

	public static void dumpScope(Obj head) {
		System.out.println("--------------");
		for (Obj o = head; o != null; o = o.next) dumpObj(o);
		for (Obj o = head; o != null; o = o.next)
			if (o.kind == Obj.Meth || o.kind == Obj.Prog) dumpScope(o.locals);
	}


	public static void init() {  // build the universe
		Obj o;
		curScope = new Scope();
		curScope.outer = null;
		curLevel = -1;

		// create predeclared types
		intType = new Struct(Struct.Int);
		charType = new Struct(Struct.Char);
		nullType = new Struct(Struct.Class);
		noType = new Struct(Struct.None);
		noObj = new Obj(Obj.Var, "???", noType);

		// create predeclared objects
		insert(Obj.Type, "int", intType);
		insert(Obj.Type, "char", charType);
		insert(Obj.Con, "null", nullType);
		chrObj = insert(Obj.Meth, "chr", charType);
		chrObj.locals = new Obj(Obj.Var, "i", intType);
		chrObj.nPars = 1;
		ordObj = insert(Obj.Meth, "ord", intType);
		ordObj.locals = new Obj(Obj.Var, "ch", charType);
		ordObj.nPars = 1;
		lenObj = insert(Obj.Meth, "len", intType);
		lenObj.locals = new Obj(Obj.Var, "a", new Struct(Struct.Arr, noType));
		lenObj.nPars = 1;
	}

}
