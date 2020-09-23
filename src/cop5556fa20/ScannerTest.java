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
	public void test1() throws LexicalException {
		String input = """
    			// abcdfashdkfhjasd sdfasf asfsdf sdas// sadfasdfasfasdfas ++123$
				int a = "b";
				123423 green (GREEN)
				;;
				123423
				====""";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
	}
}
