/**
 * Scanner for the class project in COP5556 Programming Language Principles 
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

import java.util.*;
import static java.util.Map.entry;

public class Scanner {
	private char[] chars;
	private final char EOFChar = 0;
	State state = State.START;
	
	@SuppressWarnings("preview")
	public record Token(
		Kind kind,
		int pos, //position in char array.  Starts at zero
		int length, //number of chars in token
		int line, //line number of token in source.  Starts at 1
		int posInLine //position in line of source.  Starts at 1
		) {
	}
	
	@SuppressWarnings("serial")
	public static class LexicalException extends Exception {
		int pos;
		public LexicalException(String message, int pos) {
			super(message);
			this.pos = pos;
		}
		public int pos() { return pos; }
	}
	
	
	public static enum Kind {
		IDENT, INTLIT, STRINGLIT, CONST,
		KW_X/* X */,  KW_Y/* Y */, KW_WIDTH/* width */,KW_HEIGHT/* height */, 
		KW_SCREEN/* screen */, KW_SCREEN_WIDTH /* screen_width */, KW_SCREEN_HEIGHT /*screen_height */,
		KW_image/* image */, KW_int/* int */, KW_string /* string */,
		KW_RED /* red */,  KW_GREEN /* green */, KW_BLUE /* blue */,
		ASSIGN/* = */, GT/* > */, LT/* < */, 
		EXCL/* ! */, Q/* ? */, COLON/* : */, EQ/* == */, NEQ/* != */, GE/* >= */, LE/* <= */, 
		AND/* & */, OR/* | */, PLUS/* + */, MINUS/* - */, STAR/* * */, DIV/* / */, MOD/* % */, 
	    AT/* @ */, HASH /* # */, RARROW/* -> */, LARROW/* <- */, LPAREN/* ( */, RPAREN/* ) */, 
		LSQUARE/* [ */, RSQUARE/* ] */, LPIXEL /* << */, RPIXEL /* >> */,  SEMI/* ; */, COMMA/* , */,  EOF
	}

	private enum State {
		START, SYMBOL, STRING_LIT, IDENTIFIER_PART, DIGIT, COMMENT
	}
	

	/**
	 * Returns the text of the token.  If the token represents a String literal, then
	 * the returned text omits the delimiting double quotes and replaces escape sequences with
	 * the represented character.
	 * 
	 * @param token
	 * @return
	 */
	public String getText(Token token) {
		/* IMPLEMENT THIS */
		return null;
	}
	
	
	/**
	 * Returns true if the internal interator has more Tokens
	 * 
	 * @return
	 */
	public boolean hasTokens() {
		return nextTokenPos < tokens.size();
	}
	
	/**
	 * Returns the next Token and updates the internal iterator so that
	 * the next call to nextToken will return the next token in the list.
	 * 
	 * Precondition:  hasTokens()
	 * @return
	 */
	public Token nextToken() {
		return tokens.get(nextTokenPos++);
	}
	

	/**
	 * The list of tokens created by the scan method.
	 */
	private final ArrayList<Token> tokens = new ArrayList<Token>();
	

	/**
	 * position of the next token to be returned by a call to nextToken
	 */
	private int nextTokenPos = 0;

	Scanner(String inputString) {
		int len = inputString.length();

		// Input char array terminated with EOFchar of convenience.
		chars = new char[len + 1];
		chars[len] = EOFChar;
	}
	

	
	public Scanner scan() throws LexicalException {
		int pos = 0;
		int line = 1;
		int posInLine = 1;
		state = State.START;

		int startPos = pos;
		int startPosInLine = posInLine;

		// Temp values to check
		StringBuffer currentDigit = null;
		StringBuffer currentIdentifier = null;
		StringBuffer currentSymbol = null;

		while (pos < chars.length - 1) {
			char currentChar = chars[pos];

			switch (state) {
				// Handle start state
				case START -> {
					startPos = pos;
					startPosInLine = posInLine;

					switch (currentChar) {
						case '\r' -> {
							line++; posInLine = 1;
							pos++;
						}
						case '\n' -> {
							if (pos - 1 < 0  || chars[pos - 1] != '\r') {
								line++; posInLine = 1;
							}
							pos++;
						}
						case ' ', '\t', '\f' -> {
							pos++; posInLine++;
						}
						case '"' -> {
							state = State.STRING_LIT;
						}
						case 0 -> {
							tokens.add(new Token(Kind.INTLIT, startPos, 1, line, posInLine));
							pos++; posInLine++;
						}
						default -> {
							if (isIdentifierStart(currentChar)) {
								currentIdentifier = new StringBuffer();
								state = State.IDENTIFIER_PART;
							} else if (isNonZeroDigit(currentChar)) {
								currentDigit = new StringBuffer();
								state = State.DIGIT;
							} else if (isSymbol(currentChar)) {
								currentSymbol = new StringBuffer();
								state = State.SYMBOL;
							}
						}
					}
				}

				case STRING_LIT -> {
					switch (currentChar) {
						case '"' -> {
							tokens.add(new Token(Kind.STRINGLIT, startPos, pos - startPos + 1, line, startPosInLine));
							state = State.START;
							pos++; posInLine++;
						}
						case '\\' -> {
							pos++; posInLine++;
							currentChar = chars[currentChar];
							if (pos < chars.length - 1 && isEscapeSequenceSuffix(currentChar)) {
								pos++; posInLine++;
							} else {
								throw new LexicalException(
										"Unable to scan string literal due to back slash (\\)", pos - 1);
							}
						}
						default -> {
							if (isInputCharacter(currentChar)) {
								pos++; posInLine++;
							} else {
								throw new LexicalException(
										"Unable to scan string literal due to invalid input character", pos);
							}
						}
					}
				}

				case DIGIT -> {
					if (isDigit(currentChar)) {
						currentDigit.append(currentChar);
						pos++; posInLine++;
					} else {
						if (currentDigit.length() > 10) {
							throw new LexicalException("Integer is too long (Length > 10)", startPos);
						}

						long number = Long.parseLong(currentDigit.toString());
						if (number > Integer.MAX_VALUE) {
							throw new LexicalException("Integer is too long (> Integer.MAX_VALUE)", startPos);
						}
						tokens.add(new Token(Kind.INTLIT, startPos, pos - startPos, line, startPosInLine));
						currentDigit = null;
						state = State.START;
					}
				}

				case IDENTIFIER_PART -> {
					if (isIdentifierPart(currentChar)) {
						currentIdentifier.append(currentChar);
						pos++; posInLine++;
					} else {
						String currentIdentifierString = currentIdentifier.toString();
						if (constants.containsKey(currentIdentifierString)) {
							tokens.add(new Token(Kind.CONST, startPos, pos - startPos, line, startPosInLine));
						} else {
							tokens.add(new Token(reservedWordToKind.getOrDefault(currentIdentifierString, Kind.IDENT),
									startPos, pos - startPos, line, startPosInLine));
						}
						currentIdentifier = null;
						state = State.START;
					}
				}

				case SYMBOL -> {
					if (currentSymbol.length() == 2) {
						String currentSymbolString = currentSymbol.toString();

						if ("//".equals(currentSymbolString)) {
							currentSymbol = null;
							state = State.COMMENT;
							break;
						}

						if (symbolToKind.containsKey(currentSymbolString)) {
							tokens.add(new Token(symbolToKind.get(currentSymbolString),
									startPos, pos - startPos, line, startPosInLine));
							state = State.START;
							currentSymbol = null;
						} else {
							String prefix = currentSymbol.substring(0, 1);
							if (symbolToKind.containsKey(prefix)) {
								tokens.add(new Token(symbolToKind.get(currentSymbolString),
										startPos, 1, line, startPosInLine));
								state = State.START;
								currentSymbol = null;
								pos--;
							} else {
								throw new LexicalException("Unable to scan symbol " + chars[startPos], startPos);
							}
						}
					}

					if (isSymbol(currentChar)) {
						currentSymbol.append(currentChar);
					} else {
						String currentSymbolString = currentSymbol.toString();
						if (symbolToKind.containsKey(currentSymbolString)) {
							tokens.add(new Token(symbolToKind.get(currentSymbolString),
									startPos, 1, line, startPosInLine));
							state = State.START;
							currentSymbol = null;
							pos--;
						} else {
							throw new LexicalException("Unable to scan symbol " + chars[startPos], startPos);
						}
					}
				}

				case COMMENT -> {
					if (isLineTerminator(currentChar)) {
						state = State.START;
					} else {
						pos++;
						posInLine++;
					}
				}

			}
		}

		tokens.add(new Token(Kind.EOF, pos, 0, line, posInLine));
		return this;
	}

	private static boolean isRawInputCharacter(char c) {
		return c < 128;
	}

	private static boolean isLineTerminator(char c) {
		return isLineTerminatorLF(c) || isLineTerminatorCR(c);
	}

	private static boolean isLineTerminatorLF(char c) {
		return c == '\n';
	}

	private static boolean isLineTerminatorCR(char c) {
		return c == '\r';
	}

	private static boolean isWhiteSpace(char c) {
		return c == ' ' || c == '\t' || c == '\f';
	}

	private static boolean isIdentifierStart(char c) {
		return c == '$' || c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
	}

	private static boolean isIdentifierPart(char c) {
		return isIdentifierStart(c) || Character.isDigit(c);
	}

	private static boolean isNonZeroDigit(char c) {
		return Character.isDigit(c) && c != '0';
	}

	private static boolean isDigit(char c) {
		return Character.isDigit(c);
	}

	private static boolean isSymbol(char c) {
		return symbols.contains(c);
	}

	private static boolean isInputCharacter(char c) {
		return isRawInputCharacter(c) && c != '\n' && c != '\r';
	}

	private static boolean isEscapeSequenceSuffix(char c) {
		Set<Character> validEscapeSuffix = new HashSet<>(Arrays.asList(
			'b', 't', 'n', 'f', 'r', '"', '\'', '\\'
		));
		return validEscapeSuffix.contains(c);
	}

	/**
	 * precondition:  This Token is an INTLIT or CONST
	 * @throws LexicalException 
	 * 
	 * @returns the integer value represented by the token
	 */
	public int intVal(Token t) throws LexicalException {
		/* IMPLEMENT THIS */
		return 0;
	}
	
	/**
	 * Hashmap containing the values of the predefined colors.
	 * Included for your convenience.  
	 * 
	 */
	private static HashMap<String, Integer> constants;
	static {
		constants = new HashMap<String, Integer>();	
		constants.put("Z", 255);
		constants.put("WHITE", 0xffffffff);
		constants.put("SILVER", 0xffc0c0c0);
		constants.put("GRAY", 0xff808080);
		constants.put("BLACK", 0xff000000);
		constants.put("RED", 0xffff0000);
		constants.put("MAROON", 0xff800000);
		constants.put("YELLOW", 0xffffff00);
		constants.put("OLIVE", 0xff808000);
		constants.put("LIME", 0xff00ff00);
		constants.put("GREEN", 0xff008000);
		constants.put("AQUA", 0xff00ffff);
		constants.put("TEAL", 0xff008080);
		constants.put("BLUE", 0xff0000ff);
		constants.put("NAVY", 0xff000080);
		constants.put("FUCHSIA", 0xffff00ff);
		constants.put("PURPLE", 0xff800080);
	}

	/**
	 * HashSet containing the values of the predefined symbols.
	 */
	private static final HashSet<Character> symbols = new HashSet<>(Arrays.asList(
			'(', ')', '[', ']', ';', ',', '<', '=', '>', '<', '!', '?', ':', '!', '?', '+', '-', '*', '/', '%', '@',
			'#', '&', '|'
	));

	private static final Map<String, Kind> reservedWordToKind = Map.ofEntries(
			entry("X", Kind.KW_X),
			entry("Y", Kind.KW_Y),
			entry("width", Kind.KW_WIDTH),
			entry("height", Kind.KW_HEIGHT),
			entry("screen", Kind.KW_SCREEN),
			entry("screen_width", Kind.KW_SCREEN_WIDTH),
			entry("screen_height", Kind.KW_SCREEN_HEIGHT),
			entry("image", Kind.KW_image),
			entry("int", Kind.KW_int),
			entry("string", Kind.KW_string),
			entry("red", Kind.KW_RED),
			entry("green", Kind.KW_GREEN),
			entry("blue", Kind.KW_BLUE)
	);

	private static final Map<String, Kind> symbolToKind = Map.ofEntries(
			entry("(", Kind.LPAREN),
			entry(")", Kind.RPAREN),
			entry("[", Kind.LSQUARE),
			entry("]", Kind.RSQUARE),
			entry(";", Kind.SEMI),
			entry(",", Kind.COMMA),
			entry("<<", Kind.LPIXEL),
			entry(">>", Kind.RPIXEL),
			entry("=", Kind.ASSIGN),
			entry(">", Kind.GT),
			entry("<", Kind.LT),
			entry("!", Kind.EXCL),
			entry("?", Kind.Q),
			entry(":", Kind.COLON),
			entry("==", Kind.EQ),
			entry("!=", Kind.NEQ),
			entry("<=", Kind.GE),
			entry(">=", Kind.LE),
			entry("+", Kind.PLUS),
			entry("-", Kind.MINUS),
			entry("*", Kind.STAR),
			entry("/", Kind.DIV),
			entry("%", Kind.MOD),
			entry("->", Kind.RARROW),
			entry("<-", Kind.LARROW),
			entry("@", Kind.AT),
			entry("#", Kind.HASH),
			entry("&", Kind.AND),
			entry("|", Kind.OR)
	);

	/**
	 * Returns a String representation of the list of Tokens.
	 * You may modify this as desired. 
	 */
	public String toString() {
		return tokens.toString();
	}
}
