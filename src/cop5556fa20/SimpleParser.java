/**
 * Class for  for the class project in COP5556 Programming Language Principles 
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

import cop5556fa20.Scanner.LexicalException;
import cop5556fa20.Scanner.Token;

import static cop5556fa20.Scanner.*;
import static cop5556fa20.Scanner.Kind.*;

public class SimpleParser {

	@SuppressWarnings("serial")
	public static class SyntaxException extends Exception {
		final Token token;

		public SyntaxException(Token token, String message) {
			super(message);
			this.token = token;
		}

		public Token token() {
			return token;
		}

	}


	final Scanner scanner;
	Token t;


	SimpleParser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	public void parse() throws SyntaxException, LexicalException {
		program();
		if (!consumedAll()) throw new SyntaxException(scanner.nextToken(), "tokens remain after parsing");
			//If consumedAll returns false, then there is at least one
		    //token left (the EOF token) so the call to nextToken is safe. 
	}
	

	public boolean consumedAll() {
		if (scanner.hasTokens()) {
			Token t = scanner.nextToken();
			return t.kind() == Scanner.Kind.EOF;
		}
		return true;
	}


	private void program() throws SyntaxException {
		// Program ::= (Declaration SEMI | Statement SEMI)*

		while (isFirstInDeclaration() || isFirstInStatement()) {
			if (isFirstInDeclaration()) {
				declaration();
			} else if (isFirstInStatement()) {
				statement();
			} else {
				throw new SyntaxException(t, "Unable to parse the program.");
			}
			match(SEMI);
		}
	}

	private void declaration() throws SyntaxException {
		// Declaration :: =  VariableDeclaration | ImageDeclaration

		if (isFirstInVariableDeclaration()) {
			variableDeclaration();
		} else if (isFirstInImageDeclaration()) {
			imageDeclaration();
		} else {
			throw new SyntaxException(t, "Unable to parse the declaration.");
		}
	}

	private void variableDeclaration() throws SyntaxException {
		// VariableDeclaration  ::=  VarType IDENT  (  ASSIGN  Expression  | ϵ )

		if (isFirstInVarType()) {
			varType();
			match(IDENT);
			if (assertTokenKind(ASSIGN)) {
				consume();
				expression();
			}
		} else {
			throw new SyntaxException(t, "Unable to parse the variableDeclaration.");
		}

	}

	private void varType() throws SyntaxException {
		// VarType ::= KW_int | KW_string

		if (assertTokenKind(KW_int) || assertTokenKind(KW_string)) {
			consume();
		} else {
			throw new SyntaxException(t, "Unable to parse the varType.");
		}
	}

	private void imageDeclaration() throws SyntaxException {
		// ImageDeclaration ::=  KW_image  (LSQUARE Expression COMMA Expression RSQUARE | ϵ) IDENT ((LARROW  | ASSIGN ) Expression) | ϵ )

		match(KW_image);
		if (assertTokenKind(LSQUARE)) {
			consume();
			expression();
			match(COMMA);
			expression();
			match(RSQUARE);
		}
		match(IDENT);
		if (assertTokenKind(LARROW) || assertTokenKind(ASSIGN)) {
			consume();
			expression();
		}
	}

	private void statement() throws SyntaxException {
		// Statement  ::= AssignmentStatement | ImageOutStatement    | ImageInStatement  | LoopStatement

		match(IDENT);
		if (isFirstInImageOutStatement()) {
			imageOutStatement();
		} else if (isFirstInImageInStatement()) {
			imageInStatement();
		} else if (isFirstInAssignmentStatementAndLoopStatement()) {
			AssignmentStatementORLoopStatement();
		} else {
			throw new SyntaxException(t, "Unable to parse the statement.");
		}
	}

	private void imageOutStatement() throws SyntaxException {
		// ImageOutStatement ::= IDENT RARROW Expression  | IDENT RARROW KW_SCREEN  ( LSQUARE Expression COMMA Expression RSQUARE | ϵ )

		match(RARROW);
		if (isFirstInExpression()) {
			expression();
		} else if (assertTokenKind(KW_SCREEN)) {
			consume();
			if (assertTokenKind(LSQUARE)) {
				consume();
				expression();
				match(COMMA);
				expression();
				match(RSQUARE);
			}
		} else {
			throw new SyntaxException(t, "Unable to parse the imageOutStatement.");
		}
	}

	private void imageInStatement() throws SyntaxException {
		// ImageInStatement ::= IDENT LARROW Expression

		match(LARROW);
		expression();
	}

	private void AssignmentStatementORLoopStatement() throws SyntaxException {
		// AssignmentStatement ::= IDENT ASSIGN  Expression
		// LoopStatement ∷= IDENT ASSIGN STAR ConstXYSelector COLON (Expression | ϵ ) COLON  Expression

		match(ASSIGN);
		if (isFirstInExpression()) {
			assignmentStatement();
		} else if (assertTokenKind(STAR)) {
			loopStatement();
		} else {
			throw new SyntaxException(t, "Unable to parse the AssignmentStatementORLoopStatement.");
		}
	}

	private void assignmentStatement() throws SyntaxException {
		// AssignmentStatement ::= IDENT ASSIGN  Expression
		expression();
	}

	private void loopStatement() throws SyntaxException {
		// LoopStatement ∷= IDENT ASSIGN STAR ConstXYSelector COLON (Expression | ϵ ) COLON  Expression

		match(STAR);
		constXYSelector();
		match(COLON);
		if (isFirstInExpression()) {
			expression();
		}
		match(COLON);
		expression();
	}

	//make this public for convenience testing
	public void expression() throws SyntaxException {
		// Expression ::=  OrExpression  Q  Expression COLON Expression | OrExpression
		// Expression ::=  OrExpression  ( Q  Expression COLON Expression | ϵ )


		if (!isFirstInExpression()) {
			throw new SyntaxException(t, "Unable to parse the expression.");
		}

		orExpression();
		if (assertTokenKind(Q)) {
			consume();
			expression();
			match(COLON);
			expression();
		}
	}

	private void orExpression() throws SyntaxException {
		// OrExpression ::= AndExpression   (  OR  AndExpression)*

		if (!isFirstInOrExpression()) {
			throw new SyntaxException(t, "Unable to parse the orExpression.");
		}

		andExpression();
		while (assertTokenKind(OR)) {
			consume();
			andExpression();
		}
	}

	private void andExpression() throws SyntaxException {
		// AndExpression ::= EqExpression ( AND  EqExpression )*

		if (!isFirstInAndExpression()) {
			throw new SyntaxException(t, "Unable to parse the andExpression.");
		}

		eqExpression();
		while (assertTokenKind(AND)) {
			consume();
			eqExpression();
		}
	}

	private void eqExpression() throws SyntaxException {
		// EqExpression ::= RelExpression  (  (EQ | NEQ )  RelExpression )*

		if (!isFirstInEqExpression()) {
			throw new SyntaxException(t, "Unable to parse the eqExpression.");
		}

		relExpression();
		while (assertTokenKind(EQ) || assertTokenKind(NEQ)) {
			consume();
			relExpression();
		}
	}

	private void relExpression() throws SyntaxException {
		// RelExpression ::= AddExpression (  ( LT  | GT |  LE  | GE )   AddExpression)*

		if (!isFirstInRelExpression()) {
			throw new SyntaxException(t, "Unable to parse the relExpression.");
		}

		addExpression();
		while (assertTokenKind(LT) || assertTokenKind(GT) || assertTokenKind(LE) || assertTokenKind(GE)) {
			consume();
			addExpression();
		}
	}

	private void addExpression() throws SyntaxException {
		// AddExpression ::= MultExpression   (  (PLUS | MINUS ) MultExpression )*

		if (!isFirstInAddExpression()) {
			throw new SyntaxException(t, "Unable to parse the addExpression.");
		}

		multExpression();
		while (assertTokenKind(PLUS) || assertTokenKind(MINUS)) {
			consume();
			multExpression();
		}
	}

	private void multExpression() throws SyntaxException {
		// MultExpression := UnaryExpression ( ( STAR | DIV  | MOD ) UnaryExpression )*

		if (!isFirstInMultExpression()) {
			throw new SyntaxException(t, "Unable to parse the multExpression.");
		}

		unaryExpression();
		while (assertTokenKind(STAR) || assertTokenKind(DIV) || assertTokenKind(MOD)) {
			consume();
			unaryExpression();
		}
	}

	private void unaryExpression() throws SyntaxException {
		// UnaryExpression ::= (PLUS | MINUS) UnaryExpression | UnaryExpressionNotPlusMinus

		if (assertTokenKind(PLUS) || assertTokenKind(MINUS)) {
			consume();
			unaryExpression();
		} else if (isFirstInUnaryExpressionNotPlusMinus()) {
			unaryExpressionNotPlusMinus();
		} else {
			throw new SyntaxException(t, "Unable to parse the unaryExpression.");
		}
	}

	private void unaryExpressionNotPlusMinus() throws SyntaxException {
		// UnaryExpressionNotPlusMinus ::=  EXCL  UnaryExpression  | HashExpression

		if (assertTokenKind(EXCL)) {
			consume();
			unaryExpression();
		} else if (isFirstInHashExpression()) {
			hashExpression();
		} else {
			throw new SyntaxException(t, "Unable to parse the unaryExpressionNotPlusMinus.");
		}
	}

	private void hashExpression() throws SyntaxException {
		// HashExpression ∷= Primary ( HASH Attribute)*

		if (!isFirstInHashExpression()) {
			throw new SyntaxException(t, "Unable to parse the hashExpression.");
		}

		primary();
		while (assertTokenKind(HASH)) {
			consume();
			attribute();
		}
	}

	private void primary() throws SyntaxException {
		// Primary ::=  (INTLIT | IDENT | LPAREN Expression RPAREN | STRINGLIT | KW_X | KW_Y | CONSTANT |PixelConstructor | ArgExpression ) (PixelSelector | ϵ )

		// (INTLIT | IDENT | LPAREN Expression RPAREN | STRINGLIT | KW_X | KW_Y | CONSTANT |PixelConstructor | ArgExpression )
		if (assertTokenKind(INTLIT)
				|| assertTokenKind(IDENT)
				|| assertTokenKind(STRINGLIT)
				|| assertTokenKind(KW_X)
				|| assertTokenKind(KW_Y)
				|| assertTokenKind(CONST)) {
			consume();
		} else if (assertTokenKind(LPAREN)) {
			consume();
			expression();
			match(RPAREN);
		} else if (isFirstInPixelConstructor()) {
			pixelConstructor();
		} else if (isFirstInArgExpression()) {
			argExpression();
		} else {
			throw new SyntaxException(t, "Unable to parse the primary.");
		}

		// (PixelSelector | ϵ )
		if (isFirstInPixelSelector()) {
			pixelSelector();
		}
	}

	private void pixelConstructor() throws SyntaxException {
		// PixelConstructor ∷=  LPIXEL Expression COMMA Expression COMMA Expression RPIXEL

		match(LPIXEL);
		expression();
		match(COMMA);
		expression();
		match(COMMA);
		expression();
		match(RPIXEL);
	}

	private void pixelSelector() throws SyntaxException {
		// PixelSelector ∷= LSQUARE Expression COMMA Expression RSQUARE

		expression();
		match(LSQUARE);
		expression();
		match(COMMA);
		expression();
		match(RSQUARE);
	}

	private void attribute() throws SyntaxException {
		// Attribute ∷= KW_WIDTH | KW_HEIGHT | KW_RED | KW_GREEN | KW_BLUE

		if (isFirstInAttribute()) {
			consume();
		} else {
			throw new SyntaxException(t, "Unable to parse the attribute.");
		}
	}

	private void argExpression() throws SyntaxException {
		// ArgExpression ∷= AT Primary
		match(AT);
		primary();
	}

	private void constXYSelector() throws SyntaxException {
		// ConstXYSelector ::= LSQUARE KW_X COMMA KW_Y RSQUARE
		match(LSQUARE);
		match(KW_X);
		match(COMMA);
		match(KW_Y);
		match(RSQUARE);
	}


	private boolean isFirstInProgram() {
		return isFirstInDeclaration()
				|| isFirstInStatement();
	}

	private boolean isFirstInDeclaration() {
		return isFirstInVariableDeclaration()
				|| isFirstInImageDeclaration();
   }

   private boolean isFirstInVariableDeclaration() {
		return isFirstInVarType();
   }

   private boolean isFirstInVarType() {
		return assertTokenKind(KW_int)
				|| assertTokenKind(KW_string);
   }

   private boolean isFirstInImageDeclaration() {
	   return assertTokenKind(KW_image);
   }

   private boolean isFirstInStatement() {
		return assertTokenKind(IDENT);
   }

   private boolean isFirstInImageOutStatement() {
		return assertTokenKind(RARROW);
   }

   private boolean isFirstInImageInStatement() {
		return assertTokenKind(LARROW);
   }

   private boolean isFirstInAssignmentStatementAndLoopStatement() {
		return assertTokenKind(ASSIGN);
   }

   private boolean isFirstInExpression() {
		return isFirstInOrExpression();
   }

   private boolean isFirstInOrExpression() {
		return isFirstInAndExpression();
   }

   private boolean isFirstInAndExpression() {
		return isFirstInEqExpression();
   }

   private boolean isFirstInEqExpression() {
		return isFirstInRelExpression();
   }

   private boolean isFirstInRelExpression() {
		return isFirstInAddExpression();
   }

   private boolean isFirstInAddExpression() {
		return isFirstInMultExpression();
   }

   private boolean isFirstInMultExpression() {
		return isFirstInUnaryExpression();
   }

   private boolean isFirstInUnaryExpression() {
		return assertTokenKind(PLUS)
				|| assertTokenKind(MINUS)
				|| isFirstInUnaryExpressionNotPlusMinus();
   }

   private boolean isFirstInUnaryExpressionNotPlusMinus() {
		return assertTokenKind(EXCL)
				|| isFirstInHashExpression();
   }

   private boolean isFirstInHashExpression() {
		return isFirstInPrimary();
   }

   private boolean isFirstInPrimary() {
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

   private boolean isFirstInPixelConstructor() {
		return assertTokenKind(LPIXEL);
   }

   private boolean isFirstInPixelSelector() {
		return assertTokenKind(LSQUARE);
   }

   private boolean isFirstInAttribute() {
		return assertTokenKind(KW_WIDTH)
				|| assertTokenKind(KW_HEIGHT)
				|| assertTokenKind(KW_RED)
				|| assertTokenKind(KW_GREEN)
				|| assertTokenKind(KW_BLUE);
   }

   private boolean isFirstInArgExpression() {
		return assertTokenKind(AT);
   }

	private Token consume() {
		if (scanner.hasTokens()) {
			t = scanner.nextToken();
		} else {
			t = null;
		}
		return t;
	}

	private void match(Kind kind) throws SyntaxException {
		if (kind.equals(t.kind())) {
			consume();
		} else {
			throw new SyntaxException(t, "Unable to match token of kind: " + t.kind() + ", with kind: " + kind);
		}
	}

   private boolean assertTokenKind(Kind expected) {
		return expected.equals(t.kind());
   }
}
