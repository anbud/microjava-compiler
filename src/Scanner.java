/* MicroJava Scanner (HM 99-05-17)
   =================
*/
import java.io.*;
import java.lang.reflect.Field;

public class Scanner {
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
	
	private static char ch;
	private static int col, line, pos;
	private static Reader in;
	
	private static final char eofCh = '\u0080';
	private static final char eol = '\n';
	
	private static final String key[] = { // sorted list of keywords
		"break", "class", "else", "final", "if", "new",
		"print", "program", "read", "return", "void", "while"
	};
	private static final int keyVal[] = {
		break_, class_, else_, final_, if_, new_,
		print_, program_, read_, return_, void_, while_
	};
	
	public static void init(Reader r) {
		in = r;
		
		col = 0;
		line = 0;
		pos = 0;
	}
	
	public static void nextChar() {
		try {
			ch = (char) in.read();
			col++;
			pos++;
			
			if(ch == '\n') {
				line++;
				col = 0;
			} else if(ch == '\uffff') 
				ch = eofCh;
		} catch(Exception e) {
			ch = eofCh;
		}		
	}
	
	public static Token next() {
		while(ch <= ' ') 
			nextChar();
		
		Token t = new Token();
		t.line = line;
		t.col = col;
		t.pos = pos;
		
		if(ch >= 'A' && ch <= 'z') {
			int i = 0;
			int lex[] = new int[256];
			
			do {
				lex[i++] = ch;
				nextChar();
			} while((ch >= 'A' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_');
			
			t.string = new String(lex, 0, i);
			
			int ind = java.util.Arrays.binarySearch(key, t.string);
			
			if(ind > -1)
				t.kind = keyVal[ind];
			else
				t.kind = ident;
		} else if(ch >= '0' && ch <= '9') {
			int i = 0;
			int lex[] = new int[256]; //overkill
			
			do {
				lex[i++] = ch;
				nextChar();
			} while(ch >= '0' && ch <= '9');
			
			t.kind = number;
			try {
				t.val = Integer.parseInt(new String(lex, 0, i));
			} catch(NumberFormatException e) {
				Errors.println(line, col, "Integer out of bounds. (-2^31 <= i <= 2^31)");
			}
		} else if(ch == eofCh) {
			t.kind = none;
		} else if(ch == '{') {
			t.kind = lbrace;
			nextChar();
		} else if(ch == '}') {
			t.kind = rbrace;
			nextChar();
		}  else if(ch == '[') {
			t.kind = lbrack;
			nextChar();
		} else if(ch == ']') {
			t.kind = rbrack;
			nextChar();
		} else if(ch == ',') {
			t.kind = comma;
			nextChar();
		} else if(ch == '(') {
			t.kind = lpar;
			nextChar();
		} else if(ch == ')') {
			t.kind = rpar;
			nextChar();
		} else if(ch == ';') {
			t.kind = semicolon;
			nextChar();
		} else if(ch == '.') {
			t.kind = period;
			nextChar();
		} else if(ch == '%') {
			t.kind = rem;
			nextChar();
		} else if(ch == '*') {
			t.kind = times;
			nextChar();
		} else if(ch == '+') {
			nextChar();
			if(ch == '+') {
				t.kind = pplus;
				nextChar();
			} else 
				t.kind = plus;			
		} else if(ch == '-') {
			nextChar();
			if(ch == '-') {
				t.kind = mminus;
				nextChar();
			} else 
				t.kind = minus;			
		} else if(ch == '/') {
			nextChar();
			if(ch == '/') {
				//komentar
				while(ch != eol && ch != eofCh) nextChar();
				
				t = next();
			} else 
				t.kind = slash;			
		} else if(ch == '=') {
			nextChar();
			if(ch == '=') {
				t.kind = eql;
				nextChar();
			} else 
				t.kind = assign;			
		} else if(ch == '&') {
			nextChar();
			if(ch == '&') {
				t.kind = and;
				nextChar();
			} else 
				t.kind = none;			
		} else if(ch == '|') {
			nextChar();
			if(ch == '|') {
				t.kind = or;
				nextChar();
			} else 
				t.kind = none;			
		} else if(ch == '>') {
			nextChar();
			if(ch == '=') {
				t.kind = geq;
				nextChar();
			} else 
				t.kind = gtr;			
		} else if(ch == '<') {
			nextChar();
			if(ch == '=') {
				t.kind = leq;
				nextChar();
			} else 
				t.kind = lss;			
		} else if(ch == '!') {
			nextChar();
			if(ch == '=') {
				t.kind = neq;
				nextChar();
			} else 
				t.kind = none;			
		} else if(ch == '\'') {
			nextChar();
			char c = ch;
			nextChar();
			if(ch == '\'') {
				t.kind = charCon;
				t.val = c;
				nextChar();
			} else 
				t.kind = none;			
		} else {
			t.kind = none;
			nextChar();
		}
		
		return t;
	}

	public static void main(String[] args) {
		try {
			Scanner.init(new BufferedReader(new FileReader(new File("lol.mj"))));
			
			for(;;) {
				Token t = Scanner.next();
				
				if(t.kind != none)
					System.out.println(t);
				else
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String tokenify(int id) {
		for(Field f : Scanner.class.getDeclaredFields()) {
			try {
				if(f.getInt(null) == id)
					return f.getName().replace("_", "");
			} catch(Exception e) {}
		}
		
		return "";
	}
}


