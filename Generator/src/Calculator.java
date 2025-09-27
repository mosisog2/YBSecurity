public class Calculator {
	String s;
	int pos = -1;
	char ch;

	void nextChar() {
		ch = (++pos < s.length()) ? s.charAt(pos) : (char) - 1;
	}
	boolean isNextOp(int op) {
		while (Character.isWhitespace(ch)) 
			nextChar();
		if (ch == op) {
			nextChar();
			return true;
		}
		return false;
	}
	
	public int calculate(String s) {
		this.s = s;
		this.pos = -1;
		nextChar();
		return parseExpr();
	}
	
	//expr => term | term '+''-' term
	int parseExpr() {
		int x = parseTerm();
		for (;;) {
			if (isNextOp('+')) 
				x += parseTerm();
			else if (isNextOp('-'))
				x -= parseTerm();
			else return x;
		}
	}

	//term => factor | factor '*''/' factor
	int parseTerm() {
		int x = parseFactor();
		for(;;) {
			if (isNextOp('*'))
				x *= parseFactor();
			else if (isNextOp('/'))
				x /= parseFactor();
			else return x;
		}
	}

	//factor => '+''-' factor | '(' expr ')' | number | number '^' factor
	int parseFactor() {
		if (isNextOp('+'))
			return parseFactor();
		if (isNextOp('-'))
			return -parseFactor();
		int x;
		int startPos = this.pos;
		if (isNextOp('(')) {
			x = parseExpr();
			if (!isNextOp(')')) throw new RuntimeException("missing )");
		} else if (Character.isDigit(ch)) {
			while (Character.isDigit(ch)) 
				nextChar();
			x = Integer.parseInt(s.substring(startPos, this.pos).trim());
		} else {
			throw new RuntimeException("Syntax error: " + (char) ch);
		}
		if (isNextOp('^'))
			x = (int) Math.pow(x, parseFactor());
		return x;
	}
	
	public static void calcAndPrint(Calculator c, String expr) {
		System.out.printf("%s is %d\n", expr, c.calculate(expr));
	}

	public static void main(String ... args) {
		Calculator c = new Calculator();
		calcAndPrint(c, "6 / 2 * (1 + 2)");
		calcAndPrint(c, "3 * (5 + 20 / 2 * 5)");

	}
}

