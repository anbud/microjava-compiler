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

import java.util.*;

public class Parser {
	private static final int  // token codes
		none      = 0,
		ident     = 1,
		number    = 2,
		charCon   = 3,
		plus      = 4,
		minus     = 5,
		times     = 6,
		slash     = 7,
		rem       = 8,
		eql       = 9,
		neq       = 10,
		lss       = 11,
		leq       = 12,
		gtr       = 13,
		geq       = 14,
		and       = 15,
		or        = 16,
		assign    = 17,
		pplus     = 18,
		mminus    = 19,
		semicolon = 20,
		comma     = 21,
		period    = 22,
		lpar      = 23,
		rpar      = 24,
		lbrack    = 25,
		rbrack    = 26,
		lbrace    = 27,
		rbrace    = 28,
		break_    = 29,
		class_    = 30,
		else_     = 31,
		final_    = 32,
		if_       = 33,
		new_      = 34,
		print_    = 35,
		program_  = 36,
		read_     = 37,
		return_   = 38,
		void_     = 39,
		while_    = 40,
		eof       = 41;
	
	private static final String[] name = { // token names for error messages
	"none", "indentifier", "number", "char constant", "+", "-", "*", "/", "%",
	"==", "!=", "<", "<=", ">", ">=", "&&", "||", "=", "++", "--", ";", ",",
	".", "(", ")", "[", "]", "{", "}", "break", "class", "else", "final", "if",
	"new", "print", "program", "read", "return", "void", "while"
	};

	private static Token t;			// current token (recently recognized)
	private static Token la;		// lookahead token
	private static int sym;			// always contains la.kind
	private static int lastErrPos;	// last error position (avoid spurious errors)
	
	private static Struct curMethod;
	private static int loopDepth;

	private static BitSet exprStart, statStart, statFollow, declStart, declFollow;
	// private static Obj method;		  // currently compiled method
	// private static Struct intType;  // shortcut for Tab.intType
	// private static Struct charType; // shortcut for Tab.charType


	public static void parse() {
		BitSet s;
		// initialize symbol sets
		lastErrPos = -10;
		s = new BitSet(64); exprStart = s;
		s.set(ident); s.set(number); s.set(charCon); s.set(new_); s.set(lpar); s.set(minus);

		s = new BitSet(64); statStart = s;
		s.set(ident); s.set(if_); s.set(while_); s.set(break_); s.set(read_);
		s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

		s = new BitSet(64); statFollow = s;
		s.set(else_); s.set(rbrace); s.set(eof);

		s = new BitSet(64); declStart = s;
		s.set(final_); s.set(ident); s.set(class_);

		s = new BitSet(64); declFollow = s;
		s.set(lbrace); s.set(void_); s.set(eof);
		// start parsing
		// Tab.init(); Code.init();
		// intType = Tab.intType; charType = Tab.charType;
		scan();
		Tab.init();
		Program();
		if (sym != eof) error("end of file found before end of program");
		// if (Code.mainPc < 0) semErr("program contains no 'main' method");
		// Tab.dumpScope(Tab.topScope.locals);
	}	
	
	public static void error(String msg) {
		Errors.println(Scanner.line, Scanner.col, msg);
		System.exit(0);
	}
	
	public static void scan() {
		t = la;
		la = Scanner.next();
		sym = la.kind;
	}
	
	public static void check(int exp) {
		if(exp == sym) 
			scan();
		else
			error("Expected " + name[exp]);
	}
	//The actual grammar
	
	public static void Program() {
		check(program_);
		check(ident);
		Tab.openScope();
		while(sym == final_ || sym == ident || sym == class_) 
			if(sym == final_)
				ConstDecl();
			else if(sym == ident) 
				VarDecl();
			else 
				ClassDecl();
			
		check(lbrace);
		while(sym == ident || sym == void_)
			MethodDecl();
		
		check(rbrace);
		if(Tab.find("main") == Tab.noObj)
			error("No main method.");
		
		Tab.closeScope();
	}
	
	public static void ConstDecl() {
		check(final_);
		Struct type = Type();
		check(ident);
		Obj o = Tab.insert(Obj.Con, t.string, type);
		check(assign);
		
		if(sym == number) {
			scan();
			if(type != Tab.intType)
				error("Integer expected.");
			o.val = t.val;
		} else if(sym == charCon) {
			scan();
			if(type != Tab.charType)
				error("Char expected.");
			o.val = t.val;
		} else
			error("Number or char constant expected");
		
		check(semicolon);
	}
	
	public static void VarDecl() {
		Struct type = Type();
		check(ident);
		Obj o = Tab.insert(Obj.Var, t.string, type);
		
		while(sym == comma) {
			scan();
			check(ident);
			o = Tab.insert(Obj.Var, t.string, type);
		}
		
		check(semicolon);		
	}
	
	public static void ClassDecl() {
		check(class_);
		check(ident);
		Obj o = Tab.insert(Obj.Type, t.string, new Struct(Struct.Class));
		check(lbrace);
		Tab.openScope();		
		while(sym == ident)
			VarDecl();
		
		o.type.fields = Tab.curScope.locals;
		o.type.nFields = Tab.curScope.nVars;
		
		check(rbrace);
		Tab.closeScope();
	}
	
	public static void MethodDecl() {
		Struct type = Tab.noType;
		int nPars = 0;
		
		if(sym == void_) 
			scan();
		else if(sym == ident)
			type = Type();
		else
			error("Type or void expected!");
		
		check(ident);
		Obj o = Tab.insert(Obj.Meth, t.string, type);
		check(lpar);
		Tab.openScope();
		
		if(sym == ident)
			nPars = FormPars();
		
		check(rpar);
		if(o.name.equals("main")) 
			if(nPars != 0)
				error("Main method ne moze imati parametre.");
			else if(type != Tab.noType)
				error("Main method mora biti void.");
		
		while(sym == ident)
			VarDecl();
		
		o.locals = Tab.curScope.locals;
		o.nPars = nPars;
		Block();
		Tab.closeScope();
	}
	
	public static int FormPars() {
		int nPars = 1;
		Struct type = Type();
		check(ident);
		Tab.insert(Obj.Var, t.string, type);
		
		while(sym == comma) {
			scan();
			type = Type();
			check(ident);
			Tab.insert(Obj.Var, t.string, type);
			nPars++;
		}
		
		return nPars;
	}
	
	public static Struct Type() {
		check(ident);
		Obj o = Tab.find(t.string);
		
		if(o.kind != Obj.Type)
			error("Type expected.");
		
		Struct type = o.type;
		
		if(sym == lbrack) {
			scan();
			check(rbrack);
			type = new Struct(Struct.Arr, type);
		}
		
		return type;
	}
	
	public static void Block() {
		check(lbrace);
		
		while(statStart.get(sym))
			Statement();
		
		check(rbrace);
	}
	
	public static void Statement() {
		Obj o;
		Struct type;
		if(sym == ident) {
			o = Designator();
			
			if(sym == assign) {
				scan();
				type = Expr();
				if(o.kind == Obj.Var) {
					if(!type.assignableTo(o.type)) {
						error("Incompatible types.");
					}
				} else {
					error("Designator has to be a variable.");
				}
						
			}
			else if(sym == lpar) {
				scan();
				if(o.kind != Obj.Meth)
					error("Designator has to be a method.");
				
				if(exprStart.get(sym))
					ActPars(o);
				else if(o.nPars != 0)
					error("Less paramaters than expected.");
					
				check(rpar);
			}
			else if(sym == pplus || sym == mminus) {
				scan();
				if(o.type != Tab.intType)
					error("Designator has to be an integer variable.");
			} else
				error("invalid statement");
			
			check(semicolon);
		} else if(sym == if_) {
			scan();
			check(lpar);
			Condition();
			check(rpar);
			Statement();
			
			if(sym == else_) {
				scan();
				Statement();
			}
		} else if(sym == while_) {
			scan();
			check(lpar);
			Condition();
			check(rpar);
			loopDepth++;
			Statement();
			loopDepth--;
		} else if(sym == break_) {
			scan();
			if(loopDepth == 0)
				error("break has to be in a loop.");
			check(semicolon);
		} else if(sym == return_) {
			scan();
			
			
			if(exprStart.get(sym)) {
				type = Expr();
			
				if(!type.compatibleWith(curMethod))
					error("Incompatible return value.");
			} else if(curMethod != Tab.noType)
				error("Missing return value.");
			
			check(semicolon);
		} else if(sym == read_) {
			scan();
			check(lpar);
			o = Designator();
			if(o.type != Tab.intType && o.type != Tab.charType)
				error("Invalid read paramater.");
			check(rpar);
			check(semicolon);
		} else if(sym == print_) {
			scan();
			check(lpar);
			type = Expr();
			if(type != Tab.intType && type != Tab.charType)
				error("Invalid print paramater.");
			if(sym == comma) {
				scan();
				check(number);
			}
			
			check(rpar);
			check(semicolon);
		} else if(sym == lbrace) 
			Block();
		else if(sym == semicolon)
			scan();
		else
			error("Invalid statement!");
	}
	
	public static void ActPars(Obj o) {
		Obj l = o.locals;
		Struct type;
		if(o.nPars == 0)
			error("More parameters than expected.");
		type = Expr();
		if(!type.assignableTo(l.type))
			error("Incompatible actual parameter.");
		
		int curPar = 1;
		while(sym == comma && curPar < o.nPars) {
			scan();
			type = Expr();
			
			l = l.next;
			if(!type.assignableTo(l.type))
				error("Incompatible actual parameter.");
			curPar++;
		}
		if(sym == comma)
			error("More parameters than expected.");
		else if(curPar < o.nPars)
			error("Less parameters than expected.");
	}
	
	public static void Condition() {
		CondTerm();
		
		while(sym == or) {
			scan();
			CondTerm();
		}
	}
	
	public static void CondTerm() {
		CondFact();
		
		while(sym == and) {
			scan();
			CondFact();
		}
	}
	
	public static void CondFact() {
		Struct t1 = Expr();
		int rel = Relop();
		Struct t2 = Expr();
		
		if(rel == eql || rel == neq) {
			if(!t1.compatibleWith(t2))
				error("Incompatible types.");
		} else {
			if(t1.isRefType() || t2.isRefType())
				error("Incompatible types.");
		}
	}
	
	public static int Relop() {
		if(sym == eql || sym == neq || sym == gtr || sym == geq || sym == lss ||  sym == leq) 
			scan();
		else 
			error("Invalid relation operation!");
		
		return t.kind;
	}
	
	public static Struct Expr() {
		boolean sgnMin = false;
		
		if(sym == minus) {
			scan();
			sgnMin = true;
		}
		
		Struct type = Term();
		if(sgnMin && type != Tab.intType)
			error("Minus can be applied to integers only.");
		if((sym == plus || sym == minus) && type != Tab.intType)
			error("Minus or plus can be applied to integers only.");
		while(sym == minus || sym == plus) {
			Addop();
			type = Term();
			if(type != Tab.intType)
				error("Minus or plus can be applied to integers only.");
		}
		return type;
	}
	
	public static Struct Term() {
		Struct type;
		type = Factor();
		
		if((sym == times || sym == rem || sym == slash) && type != Tab.intType)
			error("Times, slash and rem can be applied to integers only.");
		while(sym == times || sym == rem || sym == slash) {
			Mulop();
			type = Factor();			
			if(type != Tab.intType)
				error("Times, slash and rem can be applied to integers only.");
		}
		
		return type;
	}
	
	public static Struct Factor() {
		Struct type;
		Obj o;
		if(sym == ident) {
			o = Designator();
			if(sym == lpar) {
				scan();
				if(o.kind != Obj.Meth)
					error("Designator has to be a method.");
				if(exprStart.get(sym))
					ActPars(o);
				else if(o.nPars != 0)
					error("Less parameters than expected.");
				check(rpar);
				return o.type;
			}
		} else if(sym == number) {
			scan();
			return Tab.intType;
		} else if(sym == charCon) {
			scan();
			return Tab.charType;
		} else if(sym == new_) {
			scan();
			check(ident);
			o = Tab.find(t.string);
			if(o.kind == Obj.Type || o.type.kind == Struct.Arr)
				error("Type or array expected.");
			if(sym == lbrack) {
				scan();
				type = Expr();
				if(type != Tab.intType)
					error("Array index must be an integer.");
				check(rbrack);
				return new Struct(Struct.Arr, type);
			}
			return o.type;
		} else if(sym == lpar) {
			scan();
			type = Expr();
			check(rpar);
			return type;
		} else 
			error("Invalid factor!");
		
		return Tab.noType;
	}
	
	public static Obj Designator() {
		check(ident);
		Obj o = Tab.find(t.string);
		Struct type;
		while(sym == period || sym == lbrack) {
			if(sym == period) {
				scan();
				check(ident);
				return Tab.findField(t.string, o.type);
			} else {
				scan();
				type = Expr();
				check(rbrack);
				if(type != Tab.intType)
					error("Array index must be an integer.");
				if(o.type.kind != Struct.Arr) 
					error("Must be an array.");
				
				return new Obj(Obj.Var, "XXXX", o.type.elemType);
			}
		}
		
		return o;
	}
	
	public static void Addop() {
		if(sym == plus || sym == minus)
			scan();
		else
			error("Invalid addop!");
	}
	
	public static void Mulop() {
		if(sym == times || sym == slash || sym == rem)
			scan();
		else
			error("Invalid mulop!");
	}
}
