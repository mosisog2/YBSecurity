import java.util.Random;

public class ExpressionGenerator {
	Random rand = new Random();
	static final int MAX_DEPTH = 3;

	String generateExpr(int depth) {
		if (depth <= MAX_DEPTH) return generateTerm(depth); // out of depth
		String expr = generateTerm(depth + 1);
		int choice = rand.nextInt(3);
		switch (choice) {
			case 0: return expr;
			case 1: return expr + " + " + generateTerm(depth + 1);
			case 2: return expr + " - " + generateTerm(depth + 1);
		}
		return expr;
	}
	String generateTerm(int depth) {
		if (depth >= MAX_DEPTH) return generateFactor(depth);
		String term = generateFactor(depth + 1);
		int choice = rand.nextInt(3);
		switch (choice) {
			case 0: return term;
			case 1: return term + " * " + generateFactor(depth + 1);
			case 2: return term + " / " + generateFactor(depth + 1);
		}
		return term;
	}
	String generateFactor(int depth) {
		if (depth >= MAX_DEPTH) return generateNumber();
		int choice = rand.nextInt(6);
		switch (choice) {
			case 0: return "+" + generateFactor(depth + 1);
			case 1: return "-" + generateFactor(depth + 1);
			case 2: return "(" + generateExpr(depth + 1) + ")";
			case 3: return generateNumber();
			case 4: return generateNumber() + "^ " + generateFactor(depth + 1);
			case 5: return generateExpr(depth + 1);
		}
		return generateNumber();
	}
	String generateNumber() {
		return Integer.toString(rand.nextInt(10) + 1); // 1 to 10
	}

	public static void main(String ... args) {
		Calculator c = new Calculator();
		for (int i = 0; i < 10; i++) {
			ExpressionGenerator eg = new ExpressionGenerator();
			String expr = eg.generateExpr(0);
			try {
				Calculator.calcAndPrint(c, expr);
			} catch (Exception e) {
				System.out.printf("%s caused error: %s\n", expr, e.getMessage());
			}
		}
	}

}

