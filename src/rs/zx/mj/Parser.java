package kk;

import java.beans.Expression;
import java.lang.Thread.State;
import java.util.*;

import javax.xml.parsers.FactoryConfigurationError;


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

	public static int errors = 0;
	private static int errDist = 3;  
	
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
		BitSet sux = new BitSet();
		sux.set(eof);
		Program(sux);
		if (sym != eof) synErr("end of file found before end of program");
		// if (Code.mainPc < 0) semErr("program contains no 'main' method");
		// Tab.dumpScope(Tab.topScope.locals);
	}
	
	private static void scan() 
	{
		t = la;
		la = Scanner.next();
		sym = la.kind;
		errDist++;
	}
	
	private static void synErr(String s)
	{
		System.out.println(s);
		System.exit(1);
	}
	
	private static void check (int expected, BitSet sux) 
	{
		if (sym == expected) scan();  // recognized => read ahead
		else error(name[expected] + " expected", sux);
	}
	
	private static void error (String msg, BitSet sux) 
	{
//		System.out.println("line " + la.line + " col " + la.col + ": " + msg);
//		errors++;
//		while (!sux.get(sym)) scan();
		
		if (errDist >= 0) {
			System.out.println("line " + la.line + " col " + la.col + ": " + msg);
			errors++;
		}
		while (!sux.get(sym)) scan();
		errDist = 0;  // counting is restarted

	}
	
	private static BitSet add (BitSet a, BitSet b) 
	{
		BitSet c = (BitSet) a.clone();
		c.or(b);
		return c;
	}
	
//	private static BitSet add(BitSet a, int[] b)
//	{
//		BitSet c = (BitSet) a.clone();
//		for (int i : b)
//			c.set(i);
//		return c;
//	}
	
	private static BitSet add(BitSet a, int... b)
	{
		BitSet c = (BitSet) a.clone();
		for (int i : b)
			c.set(i);
		return c;
	}
	
	private static void Program(BitSet sux)
	{
//		Program 	=	"program" ident {ConstDecl | VarDecl | ClassDecl} 
//		"{" {MethodDecl} "}".
		
		BitSet bsAll = new BitSet();
		bsAll = add(bsAll, new int[] {ident, final_, class_, lbrace, void_, rbrace});
		
		check(program_, add(sux, bsAll));
		check(ident, add(sux, bsAll));
		
		BitSet bsAfterLoop = new BitSet();
		bsAfterLoop = add(bsAfterLoop, new int[] {lbrace, ident, void_, rbrace});
		
		BitSet bsBeginLoop = new BitSet();
		bsBeginLoop = add(bsBeginLoop, new int[] {final_, ident, class_});
		
		while (true)
		{
			if (bsBeginLoop.get(sym))
			{
				if (sym == final_)
					ConstDecl(add(sux, bsAll));
				else if (sym == ident)
					VarDecl(add(sux, bsAll));
				else
					ClassDecl(add(sux, bsAll));
			}
			else if (bsAfterLoop.get(sym) || sux.get(sym))
				break;
			else
				error("Greska - Program.", add(sux, bsAll));
		}
		
		check(lbrace, add(sux, new int[] {ident, void_, rbrace}));
		
		while (true)
		{
			if (sym == ident || sym == void_)
				MethodDecl(add(sux, new int[] {ident, void_, rbrace}));
			else if (sym == rbrace || sux.get(sym))
				break;
			else
				error("Greska - Program.", add(sux, new int[] {ident, void_, rbrace}));
		}
		
		check(rbrace, sux);
	}
	
	private static void ConstDecl(BitSet sux)
	{
//		ConstDecl	=	"final" Type ident "=" (number | charConst) ";".
		
		BitSet bsAll = new BitSet();
		bsAll = add(bsAll, new int[] {ident, assign, number, charCon, semicolon});
		
		check(final_, add(sux, bsAll));
		Type(add(sux, bsAll));
		
		bsAll.clear(ident);
		check(ident, add(sux, bsAll));
		
		bsAll.clear(assign);
		check(assign, add(sux, bsAll));
		
		if (sym != number && sym != charCon)
			error("Greska - ConstDecl.", add(sux, bsAll));
		
		if (sym == number || sym == charCon)
			scan();
		
		check(semicolon, sux);	
	}
	
	private static void VarDecl(BitSet sux)
	{
//		VarDecl	=	Type ident {"," ident } ";".
		
		Type(add(sux, ident, comma, semicolon));
		
		check(ident, add(sux, new int[] {comma, semicolon}));
		
		while (true)
		{
			if (sym == comma)
			{
				scan();
				check(ident, add(sux, new int[] {comma, semicolon}));
			}
			else if (sym == semicolon || sux.get(sym))
				break;
			else
				error("Greska - VarDecl - ocekivan token comma, a ne " + name[sym], add(sux, new int[] {comma, semicolon}));
		}
		
		check(semicolon, sux);
	}
	
	private static void ClassDecl(BitSet sux)
	{
//		ClassDecl	=	"class" ident "{" {VarDecl} "}".
		
		check(class_, add(sux, new int[] {ident, lbrace, rbrace}));
		
		check(ident, add(sux, new int[] {lbrace, ident, rbrace}));
		
		check(lbrace, add(sux, new int[] {ident, rbrace}));
		
		while (true)
		{
			if (sym == ident)
				VarDecl(add(sux, new int[] {ident, rbrace}));
			else if (sym == rbrace || sux.get(sym))
				break;
			else
				error("Greska - ClassDecl.", add(sux, new int[] {ident, rbrace}));
		}
		
		check(rbrace, sux);
	}
	
	private static void MethodDecl(BitSet sux)
	{
//		MethodDecl	=	(Type | "void") ident "(" [FormPars] ")" {VarDecl} Block.
		
		BitSet bsAll = new BitSet();
		bsAll = add(bsAll, new int[] {ident, void_, lpar, rpar, lbrace});
		if (sym != ident && sym != void_)
			error("Greska - MethodDecl.", add(sux, bsAll));
		
		bsAll.clear(void_); // bsAll = {ident, lpar, rpar, lbrace}
		if (sym == ident)
			Type(add(sux, bsAll));
		else if (sym == void_)
			scan();
		
		check(ident, add(sux, bsAll));
		
		bsAll.clear(lpar); // bsAll = {ident, rpar, lbrace}
		check(lpar, add(sux, bsAll));
		
		if (sym != ident && sym != rpar)
			error("Greska - FormPars.", add(sux, bsAll));
		
		if (sym == ident)
			FormPars(add(sux, bsAll));
		
		bsAll.clear(rpar); // bsAll = {ident, lbrace}
		check(rpar, add(sux, bsAll));
		
		while (true)
		{
			if (sym == ident)
				VarDecl(add(sux, bsAll));
			else if (sym == lbrace || sux.get(sym))
				break;
			else
				error("Greska - MethodDecl.", add(sux, bsAll));
		}
		
		Block(sux);
	}
	
	private static void FormPars(BitSet sux)
	{
//		FormPars	=	Type ident  {"," Type ident}.
		
		BitSet bs = new BitSet();
		bs = add(bs, new int[] {ident, comma});
		Type(add(sux, bs));
		
		BitSet bsComma = new BitSet();
		bsComma = add(bsComma, new int[] {comma});
		check(ident, add(sux, bsComma));
		
		while (true)
		{
			if (sym == comma)
			{
				scan();
				Type(add(sux, bs));
				check(ident, add(sux, bsComma));
			}
			else if (sux.get(sym))
					break;
			else
				error("Greska - FormPars", add(sux, bsComma));
		}
	}
	
	private static void Type(BitSet sux)
	{
//		Type	=	ident ["[" "]"].
		
		check(ident, add(sux, new int[] {lbrack}));
		
		if (sym != lbrack && !sux.get(sym))
			error("Greska - Type.", add(sux, new int[] {lbrack}));
		
		if (sym == lbrack)
		{
			scan();
			check(rbrack, sux);
		}
	}
	
	private static void Block(BitSet sux)
	{
//		Block	= "{" {Statement} "}".
		
		BitSet bs = new BitSet();
		bs = add(bs, statStart);
		bs.set(rbrace);
		
		check(lbrace, add(sux, bs));
		
		while (true)
		{
			if (statStart.get(sym))
				Statement(add(sux, bs));
			else if (sym == rbrace || sux.get(sym))
				break;
			else
				error("Greska - Block.", add(sux, bs));
		}
		
		check(rbrace, sux);
	}
	
	private static void Statement(BitSet sux)
	{
//		Statement	=	Designator ("=" Expr | "(" [ActPars] ")" | "++" | "--") ";"
//				|	"if" "(" Condition ")" Statement ["else" Statement]
//				|	"while" "(" Condition ")" Statement
//				|	"break" ";"
//				|	"return" [Expr] ";"
//				|	"read" "(" Designator ")" ";"
//				|	"print" "(" Expr ["," number] ")" ";"
//				|	Block
//				|	";".
		if (!statStart.get(sym))
			error("Greska - Statement.", add(sux, statStart));
		
//		Designator ("=" Expr | "(" [ActPars] ")" | "++" | "--") ";"
		
		if (sym == ident)
		{
			Designator(add(sux, new int[] {assign, lpar, pplus, mminus, semicolon}));
			
			if (sym != assign && sym != lpar && sym != pplus && sym != mminus)
				error("Greska - Statement.", add(sux, new int[] {assign, lpar, pplus, mminus, semicolon}));
			
			if (sym == assign)
			{
				scan();
				Expr(add(sux, new int[] {semicolon}));
			}
			else if (sym == lpar)
			{
				scan();
				
				if (!exprStart.get(sym) && sym != rpar)
					error("Greska - Statement.", add(add(sux, new int[] {rpar, semicolon}), exprStart));
				
				if (exprStart.get(sym))
					ActPars(add(sux, new int[] {rpar, semicolon}));
				
				check(rpar, add(sux, new int[] {semicolon}));
			}
			else if (sym == pplus || sym == mminus)
				scan();
			
			check(semicolon, sux);
		}
		
//		|	"if" "(" Condition ")" Statement ["else" Statement]
		
		else if (sym == if_)
		{
			BitSet bs = new BitSet();
			bs = add(bs, exprStart);
			bs = add(bs, statStart);
			bs = add(bs, new int[] {rpar, else_});
			
			scan();
			check(lpar, add(sux, bs));
			
			bs = add(bs, statStart);
			bs = add(bs, new int[] {rpar, else_});
			
			Condition(add(sux, bs));
			
			bs.clear(rpar);
			check(rpar, add(sux, bs));
			
			Statement(add(sux, new int[] {else_}));
			
			if (sym != else_ && !sux.get(sym))
				error("Greska - Statement.", add(sux, new int[] {else_}));
			
			if (sym == else_)
			{
				scan();
				Statement(sux);
			}	
		}
		
//		|	"while" "(" Condition ")" Statement
		
		else if (sym == while_)
		{
			BitSet bs = new BitSet();
			bs = add(bs, exprStart);
			bs = add(bs, statStart);
			bs.set(rpar);
			
			scan();
			check(lpar, add(sux, bs));
			
			bs = add(bs, statStart);
			bs.set(rpar);
			
			Condition(add(sux, bs));
			
			bs.clear(rpar);
			check(rpar, add(sux, bs));
			
			Statement(sux);
		}
		
//		|	"break" ";"
		
		else if (sym == break_)
		{
			scan();
			check(semicolon, sux);
		}
		
//		|	"return" [Expr] ";"
		
		else if (sym == return_)
		{
			scan();
			
			if (!exprStart.get(sym) && sym != semicolon)
				error("Greska - Statement.", add(add(sux, exprStart), new int[] {semicolon}));
			
			if (exprStart.get(sym))
				Expr(add(sux, new int[] {semicolon}));
			
			check(semicolon, sux);
		}
		
//		|	"read" "(" Designator ")" ";"
		
		else if (sym == read_)
		{
			scan();
			check(lpar, add(sux, new int[] {ident, rpar, semicolon}));
			Designator(add(sux, new int[] {rpar, semicolon}));
			check(rpar, add(sux, new int[] {semicolon}));
			check(semicolon, sux);
		}
		
//		|	"print" "(" Expr ["," number] ")" ";"
		
		else if (sym == print_)
		{
			scan();
			check(lpar, add(add(sux, new int[] {comma, rpar, semicolon}), exprStart));
			Expr(add(sux, new int[] {comma, rpar, semicolon}));
			
			if (sym != comma && sym != rpar)
				error("Greska - Statement.", add(sux, new int[] {comma, rpar, semicolon}));
			
			if (sym == comma)
			{
				scan();
				check(number, add(sux, new int[] {rpar, semicolon}));
			}
			check(rpar, add(sux, new int[] {semicolon}));
			check(semicolon, sux);
		}
		
//		|	Block
		
		else if (sym == lbrace)
		{
			Block(sux);
		}
		
//		|	";".
		
		else if (sym == semicolon)
			scan();		
	}
	
	private static void ActPars(BitSet sux)
	{
//		ActPars	=	Expr {"," Expr}.
		
		BitSet bs = new BitSet();
		bs.set(comma);
		
		Expr(add(sux, bs));
		
		while (true)
		{
			if (sym == comma)
			{
				scan();
				Expr(add(sux, bs));
			}
			else if (sux.get(sym))
				break;
			else
				error("Greska - ActPars.", add(sux, bs));
		}
	}
	
	private static void Condition(BitSet sux)
	{
//		Condition	=	CondTerm {"||" CondTerm}.
		
		BitSet bs = new BitSet();
		bs.set(or);
		
		CondTerm(add(sux, bs));
		
		while (true)
		{
			if (sym == or)
			{
				scan();
				CondTerm(add(sux, bs));
			}
			else if (sux.get(sym))
				break;
			else
				error("Greska - Condition.", add(sux, bs));
		}
	}
	
	private static void CondTerm(BitSet sux)
	{
//		CondTerm	=	CondFact {"&&" CondFact}.
		
		BitSet bs = new BitSet();
		bs.set(and);
		
		CondFact(add(sux, bs));
		
		while (true)
		{
			if (sym == and)
			{
				scan();
				CondFact(add(sux, bs));
			}
			else if (sux.get(sym))
				break;
			else
				error("Greska - Condition.", add(sux, bs));
		}
	}
	
	private static void CondFact(BitSet sux)
	{
//		CondFact	=	Expr Relop Expr.
		
		BitSet bs = new BitSet();
		bs.set(eql);
		bs.set(neq);
		bs.set(gtr);
		bs.set(geq);
		bs.set(lss);
		bs.set(leq);
		bs = add(exprStart, bs);
		
		Expr(add(sux, bs));
		Relop(add(sux, exprStart));
		Expr(sux);
	}
	
	private static void Relop(BitSet sux)
	{
//		Relop	=	"==" | "!=" | ">" | ">=" | "<" | "<=".
		
		BitSet bs = new BitSet();
		bs.set(eql);
		bs.set(neq);
		bs.set(gtr);
		bs.set(geq);
		bs.set(lss);
		bs.set(leq);
		
		if (!bs.get(sym))
			error("Greska - Relop.", add(sux, bs));
		
		if (bs.get(sym))
			scan();
	}
	
	private static void Expr(BitSet sux)
	{
//		Expr	=	["-"] Term {Addop Term}.
		
		BitSet bs = new BitSet();
		bs = add(bs, exprStart);
		bs.set(plus); // bs = First(Term) + {plus, minus}
		
		if (!exprStart.get(sym))
			error("Greska - Expr - Ocekivan minus, ident, number, charConst, new ili (", add(sux, bs));
		
		if (sym == minus)
			scan();
		
		Term(add(sux, new int[] {plus, minus}));
		
		while (true)
		{
			if (sym == plus || sym == minus)
			{
				Addop(add(sux, bs));
				Term(add(sux, new int[] {plus, minus}));
			}
			else if (sux.get(sym))
				break;
			else
				error("Greska - Expr - ocekivan Addop", add(sux, new int[] {plus, minus}));
		}
	}
	
	private static void Term(BitSet sux)
	{
//		Term	=	Factor {Mulop Factor}.
		
		BitSet bsMulop = new BitSet();
		bsMulop = add(bsMulop, new int[] {times, slash, rem});
		
		Factor(add(sux, bsMulop));
		
		BitSet bsFactor = new BitSet();
		bsFactor = add(bsFactor, exprStart);
		bsFactor.clear(minus);
		bsFactor = add(bsFactor, bsMulop);
		
		while (true)
		{
			if (bsMulop.get(sym))
			{
				Mulop(add(sux, bsFactor));
				Factor(add(sux, bsMulop));
			}
			else if (sux.get(sym))
				break;
			else
				error("Greska - Term.", add(sux, bsMulop));
		}
	}
	
	private static void Factor(BitSet sux)
	{
//		Factor	=	Designator ["(" [ActPars] ")"]
//				|	number
//				|	charConst
//				|	"new" ident ["[" Expr "]"]
//				|	"(" Expr ")".
		
		BitSet bs = new BitSet();
		bs = add(bs, new int[] {ident, number, charCon, new_, lpar});
		
		if (!bs.get(sym))
			error("Greska - Factor - ocekivan ident, number, charConst, new ili (", add(sux, bs));
		
//		Designator ["(" [ActPars] ")"]
		
		if (sym == ident)
		{
			Designator(add(sux, new int[] {lpar}));
			
			if (sym != lpar && !sux.get(sym))
				error("Greska - Factor.", add(sux, new int[] {lpar}));
			
			if (sym == lpar)
			{
				scan();
				if (!exprStart.get(sym) && sym != rpar)
					error("Greska - Factor.", add(add(sux, new int[] {rpar}), exprStart));
				
				if (exprStart.get(sym))
					ActPars(add(sux, new int[] {rpar}));
				
				check(rpar, sux);
			}
		}
		
//		|	number |	charConst
		
		else if (sym == number || sym == charCon)
			scan();
		
//		|	"new" ident ["[" Expr "]"]
		
		else if (sym == new_)
		{
			scan();
			check(ident, add(sux, new int[] {lbrack}));
			
			if (sym != lbrack && !sux.get(sym))
				error("Greska - Factor.", add(sux, new int[] {lbrack}));
			
			if (sym == lbrack)
			{
				scan();
				Expr(add(sux, new int[] {rbrack}));
				check(rbrack, sux);
			}
			
		}
		
//		|	"(" Expr ")".
		
		else if (sym == lpar)
		{
			scan();
			Expr(add(sux, new int[] {rpar}));
			check(rpar, sux);
		}

	}
	
	private static void Designator(BitSet sux)
	{
//		Designator	=	ident {"." ident | "[" Expr "]"}.
		
		BitSet bsBeginLoop = new BitSet();
		bsBeginLoop = add(bsBeginLoop, new int[] {period, lbrack});
		check(ident, add(sux, bsBeginLoop));
		
		while (true)
		{
			if (sym == period || sym == lbrack)
			{
				if (sym == period)
				{
					scan();
					check(ident, add(sux, bsBeginLoop));
				}
				else
				{
					scan();
					Expr(add(sux, new int[] {rbrack, period, lbrack}));
					check(rbrack, add(sux, bsBeginLoop));
				}
			}
			else if (sux.get(sym))
				break;
			else
				error("Greska - Designator.", add(sux, bsBeginLoop));
		}
	}
	
	private static void Addop(BitSet sux)
	{
//		Addop	=	"+" | "-".
		
		if (sym != plus && sym != minus)
			error("Greska - Addop.", add(sux, new int[] {plus, minus}));
		
		if (sym == plus || sym == minus)
			scan();
	}
	
	private static void Mulop(BitSet sux)
	{
//		Mulop	=	"*" | "/" | "%".
		
		if (sym != times && sym != slash && sym != rem)
			error("Greska - Mulop.", add(sux, new int[] {times, slash, rem}));
		
		if (sym == times || sym == slash || sym == rem)
			scan();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	

}
