public class Token {
	public int kind;		// token kind
	public int line;		// token line
	public int col;			// token column
	public int pos;			// token position from start of source file
	public int val;			// token value (for number and charConst)
	public String string;	// token string

	public String toString() {
		return "line " + line + ", column " + col + " " + Scanner.tokenify(kind);
	}
}