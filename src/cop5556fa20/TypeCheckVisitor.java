package cop5556fa20;

import cop5556fa20.AST.*;
import cop5556fa20.Scanner.Token;
import cop5556fa20.Scanner.Kind;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TypeCheckVisitor implements ASTVisitor {
	
	Map<String, Dec> symbolTable;

	@SuppressWarnings("serial")
	class TypeException extends Exception {
		Token first;
		String message;
		
		public TypeException(Token first, String message) {
			super();
			this.first = first;
			this.message = "Semantic error:  "+first.line() + ":" + first.posInLine() + " " + message;
		}
		
		public String toString() {
			return message;
		}	
	}
	
	
	public TypeCheckVisitor() {
		super();
		symbolTable =  new HashMap<>();

		// (TODO) Review This
		symbolTable.put("X", new DecVar(null, Type.Int, "X", null));
		symbolTable.put("Y", new DecVar(null, Type.Int, "Y", null));
	}

	@Override
	public Object visitDecImage(DecImage decImage, Object arg) throws Exception {
		// DecImage ∷= Type ( Expression0 Expression1 | ϵ) IDENT ( OP Expression2  | ϵ )

		Token first = decImage.first();
		String name = decImage.name();

		Type t0 = (Type) decImage.width().visit(this, Type.Int);
		Type t1 = (Type) decImage.height().visit(this, Type.Int);


		Kind op = decImage.op();

		// name has not been previously declared.
		assertNameNotAlreadyDeclared(first, name);

		// Expression0 is Int or Void.
		assertTypes(first, t0, Type.Int, Type.Void);

		// Expression1 is Int or Void.
		assertTypes(first, t1, Type.Int, Type.Void);

		// Expression0.type == Expression1.type
		assertTypes(first, t0, t1);

		// If (OP = LARROW) Expression2.type == String,
		// else if (OP  = ASSIGN) then Expression2.type == Image,
		// else OP == NOP.
		assertKind(first, op, Kind.LARROW, Kind.ASSIGN, Kind.NOP);

		if (isKind(op, Kind.LARROW)) {
			Type t2 = (Type) decImage.source().visit(this, Type.String);
			assertTypes(first, t2, Type.String);
		} else if (isKind(op,Kind.ASSIGN)) {
			Type t2 = (Type) decImage.source().visit(this, null);
			assertTypes(first, t2, Type.Image);
		}

		// Effect: (name,dec) added to symbol table
		symbolTable.put(name, decImage);

		return Type.Image;
	}

	@Override
	public Object visitDecVar(DecVar decVar, Object arg) throws Exception {
		// DecVar ::= Type IDENT (Expression | ϵ )

		Token first = decVar.first();
		String name = decVar.name();

		Type type = decVar.type();

		Type t = (Type) decVar.expression().visit(this, type);

		// Name (of IDENT) has not been previously declared.
		assertNameNotAlreadyDeclared(first, name);

		// Type == Expression.type
		assertTypes(first, t, type, Type.Void);

		// Effect:  (name,dec) added to symbol table
		symbolTable.put(name, decVar);

		return null;
	}

	@Override
	public Object visitExprArg(ExprArg exprArg, Object arg) throws Exception {

		Token first = exprArg.first();

		Type t = (Type) exprArg.e().visit(this, Type.Int);

		// Expression.type == Int
		assertTypes(first, t, Type.Int);

		// ExprArg.type = expected type from context (which must be String or Int)
		assertTypes(first, (Type) arg, Type.Int, Type.String);
		exprArg.setType((Type) arg);

		return exprArg.type();
	}

	@Override
	public Object visitExprBinary(ExprBinary exprBinary, Object arg) throws Exception {
		// BinaryExpr ::= Expression0 OP Expression1

		Token first = exprBinary.first();
		// Type t0 = (Type) exprBinary.e0().visit(this, arg);
		Kind op = exprBinary.op();
		// Type t1 = (Type) exprBinary.e1().visit(this, arg);

		Type t0 = (Type) exprBinary.e0().visit(this, arg);
		Type t1 = (Type) exprBinary.e1().visit(this, arg);

		// If OP == AND or OR) then Expression0.type == Boolean, Expression1.type  == Boolean
		if (isKind(op, Kind.AND, Kind.OR)) {
			assertTypes(first, t0, Type.Boolean);
			assertTypes(first, t1, Type.Boolean);

			// BinaryExpr.type = Boolean
			exprBinary.setType(Type.Boolean);

			return exprBinary.type();
		}

		// If OP == EQ or NEQ then Expression0.type == Expression1.type
		if (isKind(op, Kind.EQ, Kind.NEQ)) {
			assertTypes(first, t0, t1);

			// BinaryExpr.type = Boolean
			exprBinary.setType(Type.Boolean);

			return exprBinary.type();
		}

		// If OP == LT, GT, LE, GE then Expression0.type == Expression1.type, Expression0.type == Int
		if (isKind(op, Kind.LT, Kind.GT, Kind.LE, Kind.GE)) {
			assertTypes(first, t0, t1);
			assertTypes(first, t0, Type.Int);

			// BinaryExpr.type = Boolean
			exprBinary.setType(Type.Boolean);

			return exprBinary.type();
		}

		// Expression0.type == Expression1.type
		// IF OP == PLUS, Expression0.type == Int or String
		// else if OP == MINUS Expression0.type = Int
		if (isKind(op, Kind.PLUS, Kind.MINUS)) {
			assertTypes(first, t0, t1);

			if (isKind(op, Kind.PLUS)) {
				assertTypes(first, t0, Type.Int, Type.String);
			} else if (isKind(op, Kind.MINUS)) {
				assertTypes(first, t0, Type.Int);
			}

			// BinaryExpr.type == Expression0.type
			exprBinary.setType(t0);

			return exprBinary.type();
		}


		// Expression0.type == Expression1.type
		// Expression0.type == Int
		if (isKind(op, Kind.STAR, Kind.DIV, Kind.MOD)) {
			assertTypes(first, t0, t1);
			assertTypes(first, t0, Type.Int);

			// BinaryExpr.type = Int
			exprBinary.setType(Type.Int);

			return exprBinary.type();
		}

		// Unreachable code
		return null;
	}

	@Override
	public Object visitExprConditional(ExprConditional exprConditional, Object arg) throws Exception {
		// ExprConditional ::= Expression0 Expression1 Expression2
		// Expression0 = condition, Expression1 =trueCase, Expression2 = falseCase

		Token first = exprConditional.first();

		Type t0 = (Type) exprConditional.condition().visit(this, null);
		Type t1 = (Type) exprConditional.trueCase().visit(this, arg);
		Type t2 = (Type) exprConditional.falseCase().visit(this, arg);

		// Expression0.type == Boolean
		assertTypes(first, t0, Type.Boolean);

		// Expression1.type == Expression2.type
		assertTypes(first, t1, t2);

		exprConditional.setType(t1);

		return exprConditional.type();
	}

	@Override
	public Object visitExprConst(ExprConst exprConst, Object arg) throws Exception {

		// ExprConst.type = Int
		exprConst.setType(Type.Int);

		return exprConst.type();
	}

	@Override
	public Object visitExprHash(ExprHash exprHash, Object arg) throws Exception {
		// ExprHash ::= Expression Attribute

		Token first = exprHash.first();

		Type t = (Type) exprHash.e().visit(this, Type.Int);
		String attr = exprHash.attr();

		// Expression.type == Int or Image
		assertTypes(first, t, Type.Int, Type.Image);

		// If Expression.type == Int, Attribute == “red”,”green”, or “blue”
		if (isType(t, Type.Int)) {
			if (!("red".equals(attr) || "green".equals(attr) || "blue".equals(attr))) {
				String errorMessage = "Expected attribute value to be red, green or blue, but found " +  attr;
				throw new TypeException(first, errorMessage);
			}
		}

		// else if Expressionltype == Image, Attribute == “width” or “height”
		if (isType(t, Type.Image)) {
			if (!("width".equals(attr) || "height".equals(attr))) {
				String errorMessage = "Expected attribute value to be width or height, but found " +  attr;
				throw new TypeException(first, errorMessage);
			}
		}

		exprHash.setType(Type.Int);

		return exprHash.type();
	}

	@Override
	public Object visitExprIntLit(ExprIntLit exprIntLit, Object arg) throws Exception {
		exprIntLit.setType(Type.Int);

		return exprIntLit.type();
	}

	@Override
	public Object visitExprPixelConstructor(ExprPixelConstructor exprPixelConstructor, Object arg) throws Exception {
		// ExprPixelConstructor ::= Expressionr Expressiong Expressionb

		Token first = exprPixelConstructor.first();

		Type tR = (Type) exprPixelConstructor.redExpr().visit(this, Type.Int);
		Type tG = (Type) exprPixelConstructor.greenExpr().visit(this, Type.Int);
		Type tB = (Type) exprPixelConstructor.blueExpr().visit(this, Type.Int);

		// Expressionr.type == Expressiong.type == Expressionb.type == Int
		assertTypes(first, tR, Type.Int);
		assertTypes(first, tG, Type.Int);
		assertTypes(first, tB, Type.Int);

		// ExprPixelConstructor.type = Int
		exprPixelConstructor.setType(Type.Int);

		return exprPixelConstructor.type();
	}

	@Override
	public Object visitExprPixelSelector(ExprPixelSelector exprPixelSelector, Object arg) throws Exception {
		// ExprPixelSelector ::= Expression ExpressionX  ExpressionY

		Token first = exprPixelSelector.first();

		Type t = (Type) exprPixelSelector.image().visit(this, null);
		Type tX = (Type) exprPixelSelector.X().visit(this, Type.Int);
		Type tY = (Type) exprPixelSelector.Y().visit(this, Type.Int);

		// Expression.type == Image
		assertTypes(first, t, Type.Image);

		// Expressionx.type == Expressiony.type == Int
		assertTypes(first, tX, Type.Int);
		assertTypes(first, tY, Type.Int);

		// ExprPixelSelector.type = Int
		exprPixelSelector.setType(Type.Int);

		return exprPixelSelector.type();
	}

	@Override
	public Object visitExprStringLit(ExprStringLit exprStringLit, Object arg) throws Exception {
		exprStringLit.setType(Type.String);

		return exprStringLit.type();
	}

	@Override
	public Object visitExprUnary(ExprUnary exprUnary, Object arg) throws Exception {
		// ExprUnary ::= OP Expression

		Token first = exprUnary.first();
		Kind op = exprUnary.op();



		// If OP = PLUS or MINUS Expression.type == Int
		if (isKind(op, Kind.PLUS, Kind.MINUS)) {
			Type t = (Type) exprUnary.e().visit(this, Type.Int);

			assertTypes(first, t, Type.Int);

			// ExprUnary.type = Int
			exprUnary.setType(Type.Int);

			return exprUnary.type();
		}

		// if OP == EXCL Expression.type == Boolean
		if (isKind(op, Kind.EXCL)) {
			Type t = (Type) exprUnary.e().visit(this, null);

			assertTypes(first, t, Type.Boolean);

			// ExprUnary.type = Boolean
			exprUnary.setType(Type.Boolean);

			return exprUnary.type();
		}

		// Unreachable code.
		return null;
	}

	@Override
	public Object visitExprVar(ExprVar exprVar, Object arg) throws Exception {

		Token first = exprVar.first();
		String name = exprVar.name();

		// Name has been declared
		assertNameAlreadyDeclared(first, name);

		Dec dec = symbolTable.get(name);

		// ExprVar.type = declared type
		exprVar.setType(dec.type());

		return exprVar.type();
	}


	/**
	 * First visit method that is called.  It simply visits its children and returns null if no type errors were encountered.  
	 */
	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		for(ASTNode node: program.decOrStatement()) {
			node.visit(this, arg);
		}
		return null;
	}
	

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws Exception {
		// StatementAssign ::= IDENT Expression

		Token first = statementAssign.first();
		String name = statementAssign.name();
		// name has been declared.
		assertNameAlreadyDeclared(first, name);

		Dec dec = symbolTable.get(name);

		Type t = (Type) statementAssign.expression().visit(this, dec.type());

		// IDENT.type == Expression.type
		assertTypes(first, dec.type(), t);

		return null;
	}

	@Override
	public Object visitStatementImageIn(StatementImageIn statementImageIn, Object arg) throws Exception {
		// StatementImageIn ::= IDENT Expression

		Token first = statementImageIn.first();
		String name = statementImageIn.name();

		// name has been declared.
		assertNameAlreadyDeclared(first, name);

		Dec dec = symbolTable.get(name);

		// IDENT.type == Image
		assertTypes(first, dec.type(), Type.Image);

		Type t = (Type) statementImageIn.source().visit(this, Type.String);

		// Expression.type == Image or String
		assertTypes(first, t, Type.Image, Type.String);

		return null;
	}

	@Override
	public Object visitStatementLoop(StatementLoop statementLoop, Object arg) throws Exception {
		// StatementLoop ::= IDENT (Expression0 | ϵ ) Expression1

		Token first = statementLoop.first();
		String name = statementLoop.name();

		// name has been declared.
		assertNameAlreadyDeclared(first, name);

		Dec dec = symbolTable.get(name);

		// IDENT.type == Image
		assertTypes(first, dec.type(), Type.Image);

		Type t0 = (Type) statementLoop.cond().visit(this, null);
		Type t1 = (Type) statementLoop.e().visit(this, Type.Int);



		// Expression0.type == Void or Boolean
		assertTypes(first, t0, Type.Void, Type.Boolean);

		// Expression1.type == Int
		assertTypes(first, t1, Type.Int);

		return null;
	}

	@Override
	public Object visitExprEmpty(ExprEmpty exprEmpty, Object arg) throws Exception {
		exprEmpty.setType(Type.Void);

		return exprEmpty.type();
	}

	@Override
	public Object visitStatementOutFile(StatementOutFile statementOutFile, Object arg) throws Exception {
		// StatementOutFile ::= IDENT Expression

		Token first = statementOutFile.first();
		String name = statementOutFile.name();

		// name has been declared.
		assertNameAlreadyDeclared(first, name);

		Dec dec = symbolTable.get(name);

		// IDENT.type == Image
		assertTypes(first, dec.type(), Type.Image);

		Type t = (Type) statementOutFile.filename().visit(this,  Type.String);

		// Expression.type = String
		assertTypes(first, t, Type.String);

		return null;
	}

	@Override
	public Object visitStatementOutScreen(StatementOutScreen statementOutScreen, Object arg) throws Exception {
		// StatementOutScreen ::= IDENT (Expression0 Expression1 | ϵ )

		Token first = statementOutScreen.first();
		String name = statementOutScreen.name();

		// name has been declared.
		assertNameAlreadyDeclared(first, name);

		Dec dec = symbolTable.get(name);

		// IDENT.type == Int || IDENT.type == String || IDENT.type == Image
		assertTypes(first, dec.type(), Type.Int, Type.String, Type.Image);




		// If (IDENT.type == Int || IDENT.type == String) Expression0.type == Void
		// else if (IDENT.type == Image) then Expression0.type = Int or Void
		// else error.
		if (isType(dec.type(), Type.Int, Type.String)) {
			Type t0 = (Type) statementOutScreen.X().visit(this, null);
			Type t1 = (Type) statementOutScreen.Y().visit(this, null);
			// Expression0.type == Expression1.type
			assertTypes(first, t0, t1);
			assertTypes(first, t0, Type.Void);
		} else if (isType(dec.type(), Type.Image)) {
			Type t0 = (Type) statementOutScreen.X().visit(this, Type.Int);
			Type t1 = (Type) statementOutScreen.Y().visit(this, Type.Int);
			// Expression0.type == Expression1.type
			assertTypes(first, t0, t1);
			assertTypes(first, t0, Type.Int, Type.Void);
		}

		return null;
	}

	/*
		All private methods
	*/
	private void assertNameNotAlreadyDeclared(Token first, String name) throws Exception {
		if (symbolTable.containsKey(name)) {
			String errorMessage = "Symbol" + name + " is already declared";
			throw new TypeException(first, errorMessage);
		}
	}

	private void assertNameAlreadyDeclared(Token first, String name) throws Exception {
		if (!symbolTable.containsKey(name)) {
			String errorMessage = "Name '" + name + "' is not declared";
			throw new TypeException(first, errorMessage);
		}
	}

	private boolean isType(Type actual, Type ...expectedTypes) {
		for (Type expected : expectedTypes) {
			if (actual == expected) {
				return true;
			}
		}
		return false;
	}
	private void assertTypes(Token first, Type actual, Type ...expectedTypes) throws Exception {
		if (!(isType(actual, expectedTypes))) {
			String errorMessage = "Type mismatch. Type-1: " + actual + ", Type-2:" + Arrays.toString(expectedTypes);
			throw new TypeException(first, errorMessage);
		}
	}

	private boolean isKind(Kind actual, Kind ...expectedKinds) {
		for (Kind expectedKind : expectedKinds) {
			if (actual == expectedKind) {
				return true;
			}
		}
		return false;
	}

	private void assertKind(Token first, Kind kind, Kind ...expectedKinds) throws Exception {
		if (!isKind(kind, expectedKinds)) {
			String errorMessage = "Expected kind of " + Arrays.toString(expectedKinds) + ", but found" + kind;
			throw new TypeException(first, errorMessage);
		}
	}
}
