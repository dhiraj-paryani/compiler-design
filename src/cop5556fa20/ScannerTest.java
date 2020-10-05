/**
 * Example JUnit tests for the Scanner in the class project in COP5556 Programming Language Principles
 * at the University of Florida, Fall 2020.
 *
 * This software is solely for the educational benefit of students
 * enrolled in the course during the Fall 2020 semester.
 *
 * This software, and any software derived from it,  may not be shared with others or posted to public web sites,
 * either during the course or afterwards.
 *
 *  @Beverly A. Sanders, 2020
 *
 */
package cop5556fa20;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cop5556fa20.Scanner.LexicalException;
import cop5556fa20.Scanner.Token;

import java.io.IOException;

import static cop5556fa20.Scanner.Kind.*;

@SuppressWarnings("preview") //text blocks are preview features in Java 14

class ScannerTest {

	//To make it easy to print objects and turn this output on and off.
	static final boolean doPrint = true;
	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}

	/**
	 * Retrieves the next token and checks that its kind, position, length, line, and position in line
	 * match the given parameters.
	 *
	 * @param scanner
	 * @param kind
	 * @param pos
	 * @param length
	 * @param line
	 * @param pos_in_line
	 * @return  the Token that was retrieved
	 */
	Token checkNext(Scanner scanner, Scanner.Kind kind, int pos, int length, int line, int pos_in_line) {
		Token t = scanner.nextToken();
		Token expected = new Token(kind,pos,length,line,pos_in_line);
		assertEquals(expected, t);
		return t;
	}


	/**
	 *Retrieves the next token and checks that it is an EOF token.
	 *Also checks that this was the last token.
	 *
	 * @param scanner
	 * @return the Token that was retrieved
	 */

	Token checkNextIsEOF(Scanner scanner) {
		Token token = scanner.nextToken();
		assertEquals(Scanner.Kind.EOF, token.kind());
		assertFalse(scanner.hasTokens());
		return token;
	}

	/**
	 * Simple test case with a (legal) empty program
	 *
	 * @throws LexicalException
	 */
	@Test
	public void testEmpty() throws Scanner.LexicalException {
		String input = "";  //The input is the empty string.  This is legal
		show(input);        //Display the input
		Scanner scanner = new Scanner(input).scan();  //Create a Scanner and initialize it
		show(scanner);   //Display the Scanner
		checkNextIsEOF(scanner);  //Check that the only token is the EOF token.
	}


	/**
	 * Test illustrating how to check content of tokens.
	 *
	 * @throws LexicalException
	 */
	@Test
	public void testSemi() throws Scanner.LexicalException {

		String input = """
				;;
				;;
				""";
		show(input);
		Scanner scanner = new Scanner(input).scan();
		show(scanner);
		checkNext(scanner, SEMI, 0, 1, 1, 1);
		checkNext(scanner, SEMI, 1, 1, 1, 2);
		checkNext(scanner, SEMI, 3, 1, 2, 1);
		checkNext(scanner, SEMI, 4, 1, 2, 2);
		checkNextIsEOF(scanner);
	}

	/**
	 * Another example test, this time with an ident.  While simple tests like this are useful,
	 * many errors occur with sequences of tokens, so make sure that you have more complex test cases
	 * with multiple tokens and test the edge cases.
	 *
	 * @throws LexicalException
	 */
	@Test
	public void testIdent() throws LexicalException {
		String input = "ij";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		Token t0 = checkNext(scanner, IDENT, 0, 2, 1, 1);
		assertEquals("ij", scanner.getText(t0));
		checkNextIsEOF(scanner);
	}


	/**
	 * This example shows how to test that your scanner is behaving when the
	 * input is illegal.  In this case, a String literal
	 * that is missing the closing ".
	 *
	 * In contrast to Java String literals, the text block feature simply passes the characters
	 * to the scanner as given, using a LF (\n) as newline character.  If we had instead used a
	 * Java String literal, we would have had to escape the double quote and explicitly insert
	 * the LF at the end:  String input = "\"greetings\n";
	 *
	 * assertThrows takes the class of the expected exception and a lambda with the test code in the body.
	 * The test passes if the expected exception is thrown.  The Exception object is returned and
	 * an be printed.  It should contain an appropriate error message.
	 *
	 * @throws LexicalException
	 */
	@Test
	public void failUnclosedStringLiteral() throws LexicalException {
		String input = """
				"greetings
				""";
		show(input);
		Exception exception = assertThrows(LexicalException.class, () -> {new Scanner(input).scan();});
		show(exception);
	}

	@Test
	public void commentAtTheEnd() throws LexicalException {
		String input = """
				// This is comment...""";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		checkNextIsEOF(scanner);
	}

	@Test
	public void stringLiteralNotEnd() throws LexicalException {
		String input = """
				"This is string literal...""";
		show(input);
		Exception exception = assertThrows(LexicalException.class, () -> {new Scanner(input).scan();});
		show(exception);
	}

	@Test
	public void stringLiteralContainsLineTerminator() throws LexicalException {
		String input = """
				"This is string literal...
				This is string literal Continued" Here 123 int
				""";
		show(input);
		Exception exception = assertThrows(LexicalException.class, () -> {new Scanner(input).scan();});
		show(exception);
	}

	@Test
	public void stringLiteralContainsOddDoubleQuotes() throws LexicalException {
		String input = """
				"This is string literal"This is string literal Continued"
				""";
		show(input);
		Exception exception = assertThrows(LexicalException.class, () -> {new Scanner(input).scan();});
		show(exception);
	}


	@Test
	public void intOutsideLimit() throws LexicalException {
		String input = """
				9147483649 0
				""";
		show(input);
		Exception exception = assertThrows(LexicalException.class, () -> {new Scanner(input).scan();});
		show(exception);
	}

	@Test
	public void zeroEOF() throws LexicalException {
		String input = """
				0""";

		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		checkNext(scanner, INTLIT, 0, 1, 1, 1);
		checkNextIsEOF(scanner);
	}

	@Test
	public void nonZeroEOF() throws LexicalException {
		String input = """
				12320""";

		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		Token t = checkNext(scanner, INTLIT, 0, 5, 1, 1);
		int actual = scanner.intVal(t);
		int expected = 12320;
		assertEquals(expected, actual);
		checkNextIsEOF(scanner);
	}

	@Test
	public void symbolsEOF() throws LexicalException {
		String input = """
    			===>=<=/%<!=""";

		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);

		int actualNumTokens = 0;
		while(scanner.hasTokens()) {
			scanner.nextToken();
			actualNumTokens++;
		}
		assertEquals(actualNumTokens, 9);
	}

	@Test
	public void notASCIIInsideStringLiteral() throws LexicalException {
		String input= """
    			"भारत"
				""";
		show(input);
		Exception exception = assertThrows(LexicalException.class, () -> {new Scanner(input).scan();});
		show(exception);
	}

	@Test
	public void notASCIIIAtStartState() throws LexicalException {
		String input= """
    			भारत
				""";
		show(input);
		Exception exception = assertThrows(LexicalException.class, () -> {new Scanner(input).scan();});
		show(exception);
	}

	@Test
	public void getTextWithStringLiteral() throws LexicalException {
		String input = """
				"this is string literal"    	
				""";

		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);

		Token t = checkNext(scanner, STRINGLIT, 0, 24, 1, 1);
		String actual = scanner.getText(t);
		String expected = "this is string literal";

		assertEquals(expected, actual);
	}

	@Test
	public void getTextWithStringLiteralContainingEscapeCharacters() throws LexicalException {
		String input = """
				"this is \\t string literal"    	
				""";

		show(input);
		Scanner scanner = new Scanner(input).scan();
		show(scanner);

		Token t = checkNext(scanner, STRINGLIT, 0, 27, 1, 1);
		String actual = scanner.getText(t);
		String expected = "this is \t string literal";

		assertEquals(expected, actual);
	}

	@Test
	public void getTextWithKeyword() throws LexicalException {
		String input = """
				int
				WHITE    	
				""";
		show(input);
		Scanner scanner = new Scanner(input).scan();
		show(scanner);
		Token t = checkNext(scanner, KW_int, 0, 3, 1, 1);
		assertEquals("int", scanner.getText(t));
		t = checkNext(scanner, CONST, 4, 5, 2, 1);
		assertEquals("WHITE", scanner.getText(t));
	}

	@Test
	public void checkBackSlash() throws LexicalException {
		String input = """
				"a\\\\b"
				""";
		String testOP = "a\\b";

		show(input);
		Scanner scanner = new Scanner(input).scan();
		show(scanner);

		Token t0 = scanner.nextToken();

		assertEquals(testOP, scanner.getText(t0));
	}

	@Test
	public void checkBackSlashB() throws LexicalException {
		String input = """
				"a\\bc"
				""";
		String testOP = "a\bc";

		show(input);
		Scanner scanner = new Scanner(input).scan();
		show(scanner);

		Token t0 = scanner.nextToken();

		assertEquals(testOP, scanner.getText(t0));
	}

	@Test
	public void checkStringLiteralWithEscapeF() throws LexicalException {
		String input = """
				"This is StringLit\\f"
				""";
		String expected = "This is StringLit\f";

		show(input);
		Scanner scanner = new Scanner(input).scan();
		show(scanner);

		Token t0 = scanner.nextToken();

		assertEquals(expected, scanner.getText(t0));
	}

	@Test
	public void notASymbolError() throws LexicalException {
		String input= """
    			^
				""";
		show(input);
		Exception exception = assertThrows(LexicalException.class, () -> {new Scanner(input).scan();});
		show(exception);
	}

	@Test
	public void testComment() throws LexicalException {
		String input= """
    			aaaaa//comment\n1234//comment\r\nbbbb
				""";
		show(input);
		Scanner scanner = new Scanner(input).scan();
		show(scanner);
	}

	@Test void testGetTextForBackslashB() throws LexicalException {
		String input = """
    			x=\"\\b\";
				""";
		show(input);
		Scanner scanner = new Scanner(input).scan();
		show(scanner);
		scanner.nextToken();
		scanner.nextToken();
		String stringLit = scanner.getText(scanner.nextToken());
		System.out.println("stringLit:" + stringLit);
		System.out.println(stringLit.length());
		assertEquals("\b", stringLit);
	}

	@Test
	public void testMixed() throws LexicalException {
		String input =  """
              ijBLUEc //NAVY screenX screen\nX\n "Example\\'Strin\\ng\\r\\n" 123+
              """;
		Scanner scanner = new Scanner(input).scan();
		show(scanner);
		Token t0 = checkNext(scanner, IDENT, 0, 7, 1, 1);
		assertEquals("ijBLUEc", scanner.getText(t0));
		checkNext(scanner, KW_X, 30, 1, 2, 1);
		Token t1 = checkNext(scanner, STRINGLIT, 33, 23, 3, 2);
		String text = scanner.getText(t1);
		System.out.println("Token text");
		
		assertEquals("Example\'Strin\ng\r\n", scanner.getText(t1));
		checkNext(scanner, INTLIT, 57, 3, 3, 26);
		checkNext(scanner, PLUS, 60, 1, 3, 29);
		checkNextIsEOF(scanner);
	}
}
