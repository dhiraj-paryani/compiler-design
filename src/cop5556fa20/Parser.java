/**
 * Parser for the class project in COP5556 Programming Language Principles 
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

import java.util.ArrayList;
import java.util.List;

import cop5556fa20.AST.*;
import cop5556fa20.Scanner.Kind;
import cop5556fa20.Scanner.LexicalException;
import cop5556fa20.Scanner.Token;

import static cop5556fa20.Scanner.Kind.*;

public class Parser {
	Scanner scanner; // To read scanner output
	Token t; // Current token

	@SuppressWarnings("serial")
	public static class SyntaxException extends Exception {
		final Token token;  //the token that caused an error to be discovered.

		public SyntaxException(Token token, String message) {
			super(message);
			this.token = token;
		}

		public Token token() {
			return token;
		}
	}

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken(); // establish invariant that t is always the next token to be processed
	}

	public Program parse() throws SyntaxException, LexicalException {
		Program p = program();
		matchEOF();
		return p;
	}

	private Program program() throws SyntaxException, LexicalException {
		Token first = t; //always save the current token.

		List<ASTNode> decsAndStatements = new ArrayList<>();
		while (isFirstInDeclaration() || isFirstInStatement()) {
			if (isFirstInDeclaration()) {
				Dec dec = declaration();
				decsAndStatements.add(dec);
			} else if (isFirstInStatement()) {
				Statement statement = statement();
				decsAndStatements.add(statement);
			} else {
				throw error(t, "Unable to parse the program");
			}
			match(SEMI);
		}

		return new Program(first, decsAndStatements);  //return a Program object
	}


	private Dec declaration() throws SyntaxException, LexicalException {
		if (isFirstInVariableDeclaration()) {
			return variableDeclaration();
		} else if (isFirstInImageDeclaration()) {
			return imageDeclaration();
		} else {
			throw error(t, "Unable to parse the declaration");
		}
	}


	private DecVar variableDeclaration() throws SyntaxException, LexicalException {
		Token first = t;  //always save the current token

		if (isFirstInVarType()) {
			Type type = varType();
			String name = scanner.getText(match(IDENT));

			Expression e = Expression.empty;
			if (assertTokenKind(ASSIGN)) {
				consume();
				e = expression();
			}

			return new DecVar(first, type , name, e);  //returns a DecVar object

		} else {
			throw error(t, "Unable to parse the variableDeclaration");
		}
	}


	private Type varType() throws SyntaxException, LexicalException {
		// VarType ::= KW_int | KW_string

		if (assertTokenKind(KW_int)) {
			consume();
			return Type.Int;
		} else if (assertTokenKind(KW_string)) {
			consume();
			return Type.String;
		}

		throw error(t, "Unable to parse the variableDeclaration");
	}


	private DecImage imageDeclaration() throws SyntaxException, LexicalException {
		Token first = t;  //always save the current token

		Type type;
		Expression width = Expression.empty;
		Expression height = Expression.empty;
		Kind op = NOP;
		Expression source = Expression.empty;

		match(KW_image);
		type = Type.Image;

		if (assertTokenKind(LSQUARE)) {
			consume();
			width = expression();
			match(COMMA);
			height = expression();
			match(RSQUARE);
		}

		String name = scanner.getText(match(IDENT));

		if (assertTokenKind(LARROW) || assertTokenKind(ASSIGN)) {
			op = t.kind();
			consume();

			source = expression();
		}

		return new DecImage(first, type, name, width, height, op, source);
	}


	private Statement statement() throws SyntaxException, LexicalException {
		Token first = t;  //always save the current token

		String name = scanner.getText(match(IDENT));

		if (isFirstInImageOutStatement()) {
			return imageOutStatement(first, name);
		} else if (isFirstInImageInStatement()) {
			return imageInStatement(first, name);
		} else if (isFirstInAssignmentStatementAndLoopStatement()) {
			return AssignmentStatementORLoopStatement(first, name);
		} else {
			throw error(t, "Unable to parse the statement");
		}
	}

	private Statement imageOutStatement(Token first, String name) throws SyntaxException, LexicalException {
		// ImageOutStatement ::= IDENT RARROW Expression
		// | IDENT RARROW KW_SCREEN  ( LSQUARE Expression COMMA Expression RSQUARE | ϵ )

		match(RARROW);
		if (isFirstInExpression()) {
			Expression filename = expression();
			return new StatementOutFile(first, name, filename);
		} else if (assertTokenKind(KW_SCREEN)) {
			consume();
			Expression x = Expression.empty;
			Expression y = Expression.empty;
			if (assertTokenKind(LSQUARE)) {
				consume();
				x = expression();
				match(COMMA);
				y = expression();
				match(RSQUARE);
			}
			return new StatementOutScreen(first, name, x, y);
		} else {
			throw error(t, "Unable to parse the imageOutStatement");
		}
	}

	private StatementImageIn imageInStatement(Token first, String name) throws SyntaxException, LexicalException {
		// ImageInStatement ::= IDENT LARROW Expression

		match(LARROW);
		Expression source = expression();
		return new StatementImageIn(first, name, source);
	}


	private Statement AssignmentStatementORLoopStatement(Token first, String name)
			throws SyntaxException, LexicalException {
		// AssignmentStatement ::= IDENT ASSIGN  Expression
		// LoopStatement ∷= IDENT ASSIGN STAR ConstXYSelector COLON (Expression | ϵ ) COLON  Expression

		match(ASSIGN);
		if (isFirstInExpression()) {
			return assignmentStatement(first, name);
		} else if (assertTokenKind(STAR)) {
			return loopStatement(first, name);
		} else {
			throw error(t, "Unable to parse the AssignmentStatementORLoopStatement");
		}
	}

	private StatementAssign assignmentStatement(Token first, String name) throws SyntaxException, LexicalException {
		// AssignmentStatement ::= IDENT ASSIGN  Expression
		return new StatementAssign(first, name, expression());
	}

	private StatementLoop loopStatement(Token first, String name) throws SyntaxException, LexicalException {
		// LoopStatement ∷= IDENT ASSIGN STAR ConstXYSelector COLON (Expression | ϵ ) COLON  Expression

		Expression cond = Expression.empty;

		match(STAR);
		constXYSelector();
		match(COLON);
		if (isFirstInExpression()) {
			cond = expression();
		}
		match(COLON);
		Expression e = expression();

		return new StatementLoop(first, name, cond, e);
	}

	//expression has package visibility (rather than private) to allow tests to call expression directly
	protected Expression expression() throws SyntaxException, LexicalException {
		// Expression ::=  OrExpression  Q  Expression COLON Expression | OrExpression
		// Expression ::=  OrExpression  ( Q  Expression COLON Expression | ϵ )

		Token first = t;  //always save the current token

		if (!isFirstInExpression()) {
			throw error(t, "Unable to parse the expression");
		}

		Expression expression = orExpression();

		if (assertTokenKind(Q)) {
			consume();
			Expression trueCase = expression();
			match(COLON);
			Expression falseCase = expression();
			return new ExprConditional(first, expression, trueCase, falseCase);
		} else {
			return expression;
		}
	}


	private Expression orExpression() throws SyntaxException, LexicalException {
		// OrExpression ::= AndExpression   (  OR  AndExpression)*

		Token first = t;  //always save the current token

		if (!isFirstInOrExpression()) {
			throw error(t, "Unable to parse the orExpression");
		}

		Expression e0 = andExpression();
		while (assertTokenKind(OR)) {
			Kind op = t.kind();
			consume();

			Expression e1 = andExpression();
			e0 = new ExprBinary(first, e0, op, e1);
		}

		return e0;
	}


	private Expression andExpression() throws SyntaxException,LexicalException {
		// AndExpression ::= EqExpression ( AND  EqExpression )*

		Token first = t;  //always save the current token

		if (!isFirstInAndExpression()) {
			throw error(t, "Unable to parse the andExpression");
		}

		Expression e0 = eqExpression();
		while (assertTokenKind(AND)) {
			Kind op = t.kind();
			consume();

			Expression e1 = eqExpression();
			e0 = new ExprBinary(first, e0, op, e1);
		}

		return e0;
	}


	private Expression eqExpression() throws SyntaxException, LexicalException {
		// EqExpression ::= RelExpression  (  (EQ | NEQ )  RelExpression )*

		Token first = t;  //always save the current token

		if (!isFirstInEqExpression()) {
			throw error(t, "Unable to parse the eqExpression");
		}

		Expression e0 = relExpression();
		while (assertTokenKind(EQ) || assertTokenKind(NEQ)) {
			Kind op = t.kind();
			consume();

			Expression e1 = relExpression();
			e0 = new ExprBinary(first, e0, op, e1);
		}

		return e0;
	}

	private Expression relExpression() throws SyntaxException, LexicalException {
		// RelExpression ::= AddExpression (  ( LT  | GT |  LE  | GE )   AddExpression)*

		Token first = t;  //always save the current token

		if (!isFirstInRelExpression()) {
			throw error(t, "Unable to parse the relExpression");
		}

		Expression e0 = addExpression();
		while (assertTokenKind(LT) || assertTokenKind(GT) || assertTokenKind(LE) || assertTokenKind(GE)) {
			Kind op = t.kind();
			consume();

			Expression e1 = addExpression();
			e0 = new ExprBinary(first, e0, op, e1);
		}

		return e0;
	}

	private Expression addExpression() throws SyntaxException, LexicalException {
		// AddExpression ::= MultExpression   (  (PLUS | MINUS ) MultExpression )*

		Token first = t;  //always save the current token

		if (!isFirstInAddExpression()) {
			throw error(t, "Unable to parse the addExpression");
		}

		Expression e0 = multExpression();
		while (assertTokenKind(PLUS) || assertTokenKind(MINUS)) {
			Kind op = t.kind();
			consume();

			Expression e1 = multExpression();
			e0 = new ExprBinary(first, e0, op, e1);
		}

		return e0;
	}

	private Expression multExpression() throws SyntaxException, LexicalException {
		// MultExpression := UnaryExpression ( ( STAR | DIV  | MOD ) UnaryExpression )*

		Token first = t;  //always save the current token

		if (!isFirstInMultExpression()) {
			throw error(t, "Unable to parse the multExpression");
		}

		Expression e0 = unaryExpression();
		while (assertTokenKind(STAR) || assertTokenKind(DIV) || assertTokenKind(MOD)) {
			Kind op = t.kind();
			consume();

			Expression e1 = unaryExpression();
			e0 = new ExprBinary(first, e0, op, e1);
		}

		return e0;
	}

	private Expression unaryExpression() throws SyntaxException, LexicalException {
		// UnaryExpression ::= (PLUS | MINUS) UnaryExpression | UnaryExpressionNotPlusMinus

		Token first = t;  //always save the current token

		if (assertTokenKind(PLUS) || assertTokenKind(MINUS)) {
			Kind op = t.kind();
			consume();

			return new ExprUnary(first, op, unaryExpression());
		} else if (isFirstInUnaryExpressionNotPlusMinus()) {
			return unaryExpressionNotPlusMinus();
		} else {
			throw error(t, "Unable to parse the unaryExpression");
		}
	}

	private Expression unaryExpressionNotPlusMinus() throws SyntaxException, LexicalException {
		// UnaryExpressionNotPlusMinus ::=  EXCL  UnaryExpression  | HashExpression

		Token first = t;  //always save the current token

		if (assertTokenKind(EXCL)) {
			Kind op = t.kind();
			consume();

			return new ExprUnary(first, op, unaryExpression());
		} else if (isFirstInHashExpression()) {
			return hashExpression();
		} else {
			throw error(t, "Unable to parse the unaryExpressionNotPlusMinus");
		}
	}

	private Expression hashExpression() throws SyntaxException, LexicalException {
		// HashExpression ∷= Primary ( HASH Attribute)*

		Token first = t;  //always save the current token

		if (!isFirstInHashExpression()) {
			throw error(t, "Unable to parse the hashExpression");
		}

		Expression e = primary();
		while (assertTokenKind(HASH)) {
			consume();
			String attr = attribute();
			e = new ExprHash(first, e, attr);
		}

		return e;
	}

	private Expression primary() throws SyntaxException, LexicalException {
		// Primary ::=  (INTLIT | IDENT | LPAREN Expression RPAREN | STRINGLIT | KW_X | KW_Y | CONSTANT |PixelConstructor | ArgExpression ) (PixelSelector | ϵ )

		Token first = t;  //always save the current token

		// (INTLIT | IDENT | LPAREN Expression RPAREN | STRINGLIT | KW_X | KW_Y | CONSTANT |PixelConstructor | ArgExpression )
		Expression e = switch (t.kind()) {
			case INTLIT -> {
				int value = scanner.intVal(t);
				consume();
				yield new ExprIntLit(first, value);
			}
			case IDENT, KW_X, KW_Y -> {
				String name = scanner.getText(t);
				consume();
				yield new ExprVar(first, name);
			}
			case LPAREN -> {
				consume();
				Expression expression = expression();
				match(RPAREN);
				yield expression;
			}
			case STRINGLIT -> {
				String text = scanner.getText(t);
				consume();
				yield new ExprStringLit(first, text);
			}
			case CONST -> {
				String name = scanner.getText(t);
				int value = scanner.intVal(t);

				consume();

				yield new ExprConst(first, name, value);
			}
			default -> {
				if (isFirstInPixelConstructor()) {
					yield pixelConstructor();
				} else if (isFirstInArgExpression()) {
					yield argExpression();
				} else {
					throw error(t, "Unable to parse the primary");
				}
			}
		};

		// (PixelSelector | ϵ )
		if (isFirstInPixelSelector()) {
			return pixelSelector(first, e);
		}

		return e;
	}

	private ExprPixelConstructor pixelConstructor() throws SyntaxException, LexicalException {
		// PixelConstructor ∷=  LPIXEL Expression COMMA Expression COMMA Expression RPIXEL

		Token first = t;  //always save the current token

		match(LPIXEL);
		Expression redExpr = expression();
		match(COMMA);
		Expression greenExpr = expression();
		match(COMMA);
		Expression blueExpr = expression();
		match(RPIXEL);

		return new ExprPixelConstructor(first, redExpr, greenExpr, blueExpr);
	}

	private ExprPixelSelector pixelSelector(Token first, Expression image) throws SyntaxException, LexicalException {
		// PixelSelector ∷= LSQUARE Expression COMMA Expression RSQUARE

		match(LSQUARE);
		Expression x = expression();
		match(COMMA);
		Expression y = expression();
		match(RSQUARE);

		return new ExprPixelSelector(first, image, x, y);
	}

	private String attribute() throws SyntaxException, LexicalException {
		// Attribute ∷= KW_WIDTH | KW_HEIGHT | KW_RED | KW_GREEN | KW_BLUE

		if (isFirstInAttribute()) {
			String text = scanner.getText(t);
			consume();
			return text;
		} else {
			throw error(t, "Unable to parse the attribute");
		}
	}

	private ExprArg argExpression() throws SyntaxException, LexicalException {
		// ArgExpression ∷= AT Primary

		Token first = t;  //always save the current token

		match(AT);
		return new ExprArg(first, primary());
	}

	private void constXYSelector() throws SyntaxException, LexicalException {
		// ConstXYSelector ::= LSQUARE KW_X COMMA KW_Y RSQUARE
		match(LSQUARE);
		match(KW_X);
		match(COMMA);
		match(KW_Y);
		match(RSQUARE);
	}

	protected boolean isKind(Kind kind) {
		return t.kind() == kind;
	}

	protected boolean isKind(Kind... kinds) {
		for (Kind k : kinds) {
			if (k == t.kind())
				return true;
		}
		return false;
	}

	/**
	 * Precondition: kind != EOF
	 * 
	 * @param kind
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind kind) throws SyntaxException {
		Token tmp = t;
		if (isKind(kind)) {
			consume();
			return tmp;
		}
		throw error(t, kind.toString());
	}

	/**
	 * Precondition: kind != EOF
	 *
	 * @param kind
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind... kinds) throws SyntaxException {
		Token tmp = t;
		if (isKind(kinds)) {
			consume();
			return tmp;
		}
		throw error(t, "expected one of " + kinds);
	}

	private Token consume() throws SyntaxException {
		Token tmp = t;
		if (isKind(EOF)) {
			throw error(t, "attempting to consume EOF");
		}
		t = scanner.nextToken();
		return tmp;
	}

	private SyntaxException error(Token t, String m) throws SyntaxException {
		String message = m + " at " + t.line() + ":" + t.posInLine();
		return new SyntaxException(t, message);
	}
	
	/**
	 * Only for check at end of program. Does not "consume" EOF so there is no
	 * attempt to get the nonexistent next Token.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (isKind(EOF)) {
			return t;
		}
		throw error(t, EOF.toString());
	}

	private boolean isFirstInDeclaration() throws SyntaxException {
		return isFirstInVariableDeclaration()
				|| isFirstInImageDeclaration();
	}

	private boolean isFirstInVariableDeclaration() throws SyntaxException {
		return isFirstInVarType();
	}

	private boolean isFirstInVarType() throws SyntaxException {
		return assertTokenKind(KW_int)
				|| assertTokenKind(KW_string);
	}

	private boolean isFirstInImageDeclaration() throws SyntaxException {
		return assertTokenKind(KW_image);
	}

	private boolean isFirstInStatement() throws SyntaxException {
		return assertTokenKind(IDENT);
	}

	private boolean isFirstInImageOutStatement() throws SyntaxException {
		return assertTokenKind(RARROW);
	}

	private boolean isFirstInImageInStatement() throws SyntaxException {
		return assertTokenKind(LARROW);
	}

	private boolean isFirstInAssignmentStatementAndLoopStatement() throws SyntaxException {
		return assertTokenKind(ASSIGN);
	}

	private boolean isFirstInExpression() throws SyntaxException {
		return isFirstInOrExpression();
	}

	private boolean isFirstInOrExpression() throws SyntaxException {
		return isFirstInAndExpression();
	}

	private boolean isFirstInAndExpression() throws SyntaxException {
		return isFirstInEqExpression();
	}

	private boolean isFirstInEqExpression() throws SyntaxException {
		return isFirstInRelExpression();
	}

	private boolean isFirstInRelExpression() throws SyntaxException {
		return isFirstInAddExpression();
	}

	private boolean isFirstInAddExpression() throws SyntaxException {
		return isFirstInMultExpression();
	}

	private boolean isFirstInMultExpression() throws SyntaxException {
		return isFirstInUnaryExpression();
	}

	private boolean isFirstInUnaryExpression() throws SyntaxException {
		return assertTokenKind(PLUS)
				|| assertTokenKind(MINUS)
				|| isFirstInUnaryExpressionNotPlusMinus();
	}

	private boolean isFirstInUnaryExpressionNotPlusMinus() throws SyntaxException {
		return assertTokenKind(EXCL)
				|| isFirstInHashExpression();
	}

	private boolean isFirstInHashExpression() throws SyntaxException {
		return isFirstInPrimary();
	}

	private boolean isFirstInPrimary() throws SyntaxException {
		return assertTokenKind(INTLIT)
				|| assertTokenKind(IDENT)
				|| assertTokenKind(LPAREN)
				|| assertTokenKind(STRINGLIT)
				|| assertTokenKind(KW_X)
				|| assertTokenKind(KW_Y)
				|| assertTokenKind(CONST)
				|| isFirstInPixelConstructor()
				|| isFirstInArgExpression();
	}

	private boolean isFirstInPixelConstructor() throws SyntaxException {
		return assertTokenKind(LPIXEL);
	}

	private boolean isFirstInPixelSelector() throws SyntaxException {
		return assertTokenKind(LSQUARE);
	}

	private boolean isFirstInAttribute() throws SyntaxException {
		return assertTokenKind(KW_WIDTH)
				|| assertTokenKind(KW_HEIGHT)
				|| assertTokenKind(KW_RED)
				|| assertTokenKind(KW_GREEN)
				|| assertTokenKind(KW_BLUE);
	}

	private boolean isFirstInArgExpression() throws SyntaxException {
		return assertTokenKind(AT);
	}

	private boolean assertTokenKind(Kind expected) throws SyntaxException {
		if (t == null) {
			throw new SyntaxException(null, "There is no token to assert kind");
		}
		return expected.equals(t.kind());
	}
}
