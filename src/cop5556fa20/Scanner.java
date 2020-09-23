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
	private final char[] chars;
	State state = State.START;
	private static final char EOFChar = 0;

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
		START, SYMBOL,
		STRING_LIT_START, STRING_LIT, STRING_LIT_END, STRING_LIT_ESCAPE_PREFIX, STRING_LIT_ESCAPE_SUFFIX,
		IDENTIFIER_PART,
		ZERO_DIGIT, DIGIT,
		COMMENT,
		WHITE_SPACE, LINE_TERMINATOR,
		EOF
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
		StringBuilder sb = new StringBuilder();
		switch (token.kind) {
			case STRINGLIT -> {
				boolean isPreviousBackSlash = false;
				for(int i=1; i < token.length - 1; i++) {
					if (isPreviousBackSlash) {
						sb.append(escapeSuffixToEscapeSequence.get(chars[token.pos() + i]));
						isPreviousBackSlash = false;
					} else if (chars[token.pos() + i] == '\\') {
						isPreviousBackSlash = true;
					} else {
						sb.append(chars[token.pos() + i]);
					}
				}
				return sb.toString();
			}
			default -> {
				for(int i=0; i < token.length; i++) {
					sb.append(chars[token.pos() + i]);
				}
				return sb.toString();
			}
		}
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
		chars = Arrays.copyOf(inputString.toCharArray(), len + 1);
		chars[len] = EOFChar;
	}



	public Scanner scan() throws LexicalException {
		// Initialization
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

		// Process each character at a time.
		while (pos < chars.length) {
			char currentChar = chars[pos];

			switch (state) {

				// Handle START state -- START
				case START -> {

					// Reset start state and start position in line.
					startPos = pos;
					startPosInLine = posInLine;

					if (isLineTerminator(currentChar)) {
						state = State.LINE_TERMINATOR;
					} else if (isWhiteSpace(currentChar)) {
						state = State.WHITE_SPACE;
					} else if (isStringStartCharacter(currentChar)) {
						state = State.STRING_LIT_START;
					} else if (isZeroDigit(currentChar)) {
						state = State.ZERO_DIGIT;
					} else if (isIdentifierStart(currentChar)) {
						currentIdentifier = new StringBuffer();
						state = State.IDENTIFIER_PART;
					} else if (isNonZeroDigit(currentChar)) {
						currentDigit = new StringBuffer();
						state = State.DIGIT;
					} else if (isSymbol(currentChar)) {
						currentSymbol = new StringBuffer();
						state = State.SYMBOL;
					} else if (isEOFChar(currentChar)) {
						state = State.EOF;
					} else {
						throw new LexicalException(
								"Unable to process character at position " + pos + " int start state", pos);
					}
				}
				// Handle START state -- END


				// Handle EOF state -- START
				case EOF -> {
					tokens.add(new Token(Kind.EOF, startPos, 0, line, startPosInLine));
					pos++; posInLine++;
				}
				// Handle EOF state -- END


				// Handle COMMENT state -- START
				case COMMENT -> {
					if (isLineTerminator(currentChar) || isEOFChar(currentChar)) {
						state = State.START;
					} else {
						pos++; posInLine++;
					}
				}
				// Handle COMMENT state -- END


				// Handle LINE_TERMINATOR state -- START
				case LINE_TERMINATOR -> {
					if (isLineTerminatorCR(currentChar)) {
						line++; posInLine = 1;
					} else if (isLineTerminatorLF(currentChar)) {
						if (pos - 1 < 0  || !isLineTerminatorCR(chars[pos - 1])) {
							line++; posInLine = 1;
						}
					}
					pos++;
					state = State.START;
				}
				// Handle LINE_TERMINATOR state -- END


				// Handle WHITE_SPACE state -- START
				case WHITE_SPACE -> {
					pos++; posInLine++;
					state = State.START;
				}
				// Handle WHITE_SPACE state -- END


				// Handle STRING_LIT_START state -- START
				case STRING_LIT_START -> {
					pos++; posInLine++;
					state = State.STRING_LIT;
				}
				// Handle STRING_LIT_START state -- END

				case STRING_LIT_END -> {
					tokens.add(new Token(Kind.STRINGLIT, startPos, pos - startPos + 1, line, startPosInLine));
					pos++; posInLine++;
					state = State.START;
				}

				case STRING_LIT_ESCAPE_PREFIX -> {
					pos++; posInLine++;
					state = State.STRING_LIT_ESCAPE_SUFFIX;
				}

				case STRING_LIT_ESCAPE_SUFFIX -> {
					if (isEOFChar(currentChar)) {
						throw new LexicalException(
								"Reached end of file while processing string literal inside STRING_LIT_ESCAPE_SUFFIX", startPos);
					}

					if (isEscapeSequenceSuffix(currentChar)) {
						pos++; posInLine++;
						state = State.STRING_LIT;
					} else {
						throw new LexicalException("Unable to process escape suffix inside string literal", pos);
					}
				}

				// Handle STRING_LIT state -- START
				case STRING_LIT -> {
					if (isEOFChar(currentChar)) {
						throw new LexicalException(
								"Reached end of file while processing string literal", startPos);
					}

					if (isStringEndCharacter(currentChar)) {
						state = State.STRING_LIT_END;
					} else if (isEscapeSequencePrefix(currentChar)) {
						state = State.STRING_LIT_ESCAPE_PREFIX;
					} else if (isInputCharacter(currentChar)) {
							pos++; posInLine++;
					} else {
						throw new LexicalException(
								"Unable to scan string literal due to invalid input character at position " + pos, pos);
					}
				}
				// Handle STRING_LIT state -- END


				// Handle ZERO_DIGIT state -- START
				case ZERO_DIGIT -> {
					tokens.add(new Token(Kind.INTLIT, startPos, 1, line, posInLine));
					pos++; posInLine++;
					state = State.START;
				}
				// Handle ZERO_DIGIT state -- END


				// Handle DIGIT state -- START
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
				// Handle DIGIT state -- END


				// Handle IDENTIFIER_PART state -- START
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
				// Handle IDENTIFIER_PART state -- END


				// Handle SYMBOL state -- START
				case SYMBOL -> {
					if (currentSymbol.length() == 2) {
						String currentSymbolString = currentSymbol.toString();

						if ("//".equals(currentSymbolString)) {
							currentSymbol = null;
							state = State.COMMENT;
						} else if (symbolToKind.containsKey(currentSymbolString)) {
							tokens.add(new Token(symbolToKind.get(currentSymbolString),
									startPos, pos - startPos, line, startPosInLine));
							state = State.START;
							currentSymbol = null;
						} else {
							String prefix = currentSymbol.substring(0, 1);
							if (symbolToKind.containsKey(prefix)) {
								tokens.add(new Token(symbolToKind.get(prefix),
										startPos, 1, line, startPosInLine));
								state = State.START;
								currentSymbol = null;
								pos--; posInLine--;
							} else {
								throw new LexicalException("Unable to scan symbol " + chars[startPos], startPos);
							}
						}
					} else if (isSymbol(currentChar)) {
						currentSymbol.append(currentChar);
						pos++; posInLine++;
					} else {
						String currentSymbolString = currentSymbol.toString();
						if (symbolToKind.containsKey(currentSymbolString)) {
							tokens.add(new Token(symbolToKind.get(currentSymbolString),
									startPos, 1, line, startPosInLine));
							state = State.START;
							currentSymbol = null;
						} else {
							throw new LexicalException("Unable to scan symbol " + chars[startPos], startPos);
						}
					}
				}
				// Handle SYMBOL state -- END


			}
		}

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
		return isIdentifierStart(c) || isDigit(c);
	}

	private static boolean isZeroDigit(char c) {
		return c == '0';
	}

	private static boolean isNonZeroDigit(char c) {
		return c >= '1' && c <= '9';
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private static boolean isSymbol(char c) {
		return symbols.contains(c);
	}

	private static boolean isInputCharacter(char c) {
		return isRawInputCharacter(c) && !isLineTerminator(c);
	}

	private static boolean isStringStartCharacter(char c) {
		return c == '"';
	}

	private static boolean isStringEndCharacter(char c) {
		return c == '"';
	}

	private static boolean isEscapeSequencePrefix(char c) {
		return c == '\\';
	}

	private static boolean isEscapeSequenceSuffix(char c) {
		Set<Character> validEscapeSuffix = new HashSet<>(Arrays.asList(
			'b', 't', 'n', 'f', 'r', '"', '\'', '\\'
		));
		return validEscapeSuffix.contains(c);
	}

	private static boolean isEOFChar(char c) {
		return c == EOFChar;
	}


	private static final Map<Character, Character> escapeSuffixToEscapeSequence = Map.ofEntries(
			entry('n', '\n'),
			entry('b', '\b'),
			entry('t', '\t'),
			entry('f', '\f'),
			entry('r', '\r'),
			entry('"', '\"'),
			entry('\'', '\''),
			entry('\\', '\\')
	);
	/**
	 * precondition:  This Token is an INTLIT or CONST
	 * @throws LexicalException
	 *
	 * @returns the integer value represented by the token
	 */
	public int intVal(Token t) throws LexicalException {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i < t.length; i++) {
			sb.append(chars[t.pos() + i]);
		}
		switch (t.kind) {
			case INTLIT -> {
				return Integer.parseInt(sb.toString());
			}
			case CONST -> {
				return constants.get(sb.toString());
			}
			default -> {
				throw new LexicalException("Unable to get int value for token of kind " + t.kind, t.pos);
			}
		}
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
			'(', ')', '[', ']', ';', ',', '<', '=', '>', '!', '?', ':', '+', '-', '*', '/', '%', '@',
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
			entry("(", Kind.LPAREN), entry(")", Kind.RPAREN),

			entry("[", Kind.LSQUARE), entry("]", Kind.RSQUARE),

			entry(";", Kind.SEMI),
			entry(",", Kind.COMMA),

			entry("<<", Kind.LPIXEL), entry(">>", Kind.RPIXEL),

			entry("=", Kind.ASSIGN),

			entry(">", Kind.GT), entry("<", Kind.LT),

			entry("!", Kind.EXCL),
			entry("?", Kind.Q),
			entry(":", Kind.COLON),

			entry("==", Kind.EQ), entry("!=", Kind.NEQ),

			entry(">=", Kind.GE), entry("<=", Kind.LE),

			entry("+", Kind.PLUS), entry("-", Kind.MINUS),

			entry("*", Kind.STAR), entry("/", Kind.DIV), entry("%", Kind.MOD),

			entry("->", Kind.RARROW), entry("<-", Kind.LARROW),

			entry("@", Kind.AT),
			entry("#", Kind.HASH),

			entry("&", Kind.AND), entry("|", Kind.OR)
	);

	/**
	 * Returns a String representation of the list of Tokens.
	 * You may modify this as desired.
	 */
	public String toString() {
		return tokens.toString();
	}
}
