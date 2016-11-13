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
		Program();
		if (sym != eof) synErr("end of file found before end of program");
		// if (Code.mainPc < 0) semErr("program contains no 'main' method");
		// Tab.dumpScope(Tab.topScope.locals);
	}
	
	
	
	public static void synErr(String msg) {
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
			synErr("Expected " + name[exp]);
	}
	//The actual grammar
	
	public static void Program() {
		check(program_);
		check(ident);
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
	}
	
	public static void ConstDecl() {
		check(final_);
		Type();
		check(ident);
		check(assign);
		
		if(sym == number || sym == charCon)
			scan();
		else
			synErr("Number or char constant expected");
		
		check(semicolon);
	}
	
	public static void VarDecl() {
		Type();
		check(ident);
		
		while(sym == comma) {
			scan();
			check(ident);
		}
		
		check(semicolon);		
	}
	
	public static void ClassDecl() {
		check(class_);
		check(ident);
		check(lbrace);
		
		while(sym == ident)
			VarDecl();
		
		check(rbrace);
	}
	
	public static void MethodDecl() {
		if(sym == void_) 
			scan();
		else if(sym == ident)
			Type();
		else
			synErr("Type or void expected!");
		
		check(ident);
		check(lpar);
		
		if(sym == ident)
			FormPars();
		
		check(rpar);
		
		while(sym == ident)
			VarDecl();
		
		Block();
	}
	
	public static void FormPars() {
		Type();
		check(ident);
		
		while(sym == comma) {
			scan();
			Type();
			check(ident);
		}
	}
	
	public static void Type() {
		check(ident);
		
		if(sym == lbrack) {
			scan();
			check(rbrack);
		}
	}
	
	public static void Block() {
		check(lbrace);
		
		while(statStart.get(sym))
			Statement();
		
		check(rbrace);
	}
	
	public static void Statement() {
		if(sym == ident) {
			Designator();
			
			while(sym == assign || sym == lpar || sym == pplus || sym == mminus) {
				if(sym == assign) {
					scan();
					Expr();
				} else if(sym == lpar) {
					scan();
					if(exprStart.get(sym))
						ActPars();
					check(rpar);
				} else 
					scan();
			}
			
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
			Statement();
		} else if(sym == break_) {
			scan();
			check(semicolon);
		} else if(sym == return_) {
			scan();
			
			if(exprStart.get(sym))
				Expr();
			
			check(semicolon);
		} else if(sym == read_) {
			scan();
			check(lpar);
			Designator();
			check(rpar);
			check(semicolon);
		} else if(sym == print_) {
			scan();
			check(lpar);
			Expr();
			
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
			synErr("Invalid statement!");
	}
	
	public static void ActPars() {
		Expr();
		
		while(sym == comma) {
			scan();
			Expr();
		}
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
		Expr();
		Relop();
		Expr();
	}
	
	public static void Relop() {
		if(sym == eql || sym == neq || sym == gtr || sym == geq || sym == lss ||  sym == leq) 
			scan();
		else 
			synErr("Invalid relation operation!");
	}
	
	public static void Expr() {
		if(sym == minus)
			scan();
		
		Term();
		
		while(sym == minus || sym == plus) {
			Addop();
			Term();
		}
	}
	
	public static void Term() {
		Factor();
		
		while(sym == times || sym == rem || sym == slash) {
			Mulop();
			Factor();			
		}
	}
	
	public static void Factor() {
		if(sym == ident) {
			Designator();
			if(sym == lpar) {
				scan();
				if(exprStart.get(sym))
					ActPars();
				check(rpar);
			}
		} else if(sym == number || sym == charCon)
			scan();
		else if(sym == new_) {
			scan();
			check(ident);
			if(sym == lbrack) {
				scan();
				Expr();
				check(rbrack);
			}
		} else if(sym == lpar) {
			scan();
			Expr();
			check(rpar);
		} else 
			synErr("Invalid factor!");
	}
	
	public static void Designator() {
		check(ident);
		while(sym == period || sym == lbrack) {
			scan();
			if(sym == ident)
				scan();
			else {
				Expr();
				check(rbrack);
			}
		}
	}
	
	public static void Addop() {
		if(sym == plus || sym == minus)
			scan();
		else
			synErr("Invalid addop!");
	}
	
	public static void Mulop() {
		if(sym == times || sym == slash || sym == rem)
			scan();
		else
			synErr("Invalid mulop!");
	}
}
