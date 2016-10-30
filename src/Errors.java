public class Errors {
	public static int count;

	public static void println(int line, int col, String msg) {
		System.out.println("-- line "+line+", col "+col+": "+msg);
		count++;
	}

	public static void reset(){
		count = 0;
	}
}