/**
 * This code was developed for the class project in COP5556 Programming Language Principles 
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

import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cop5556fa20.AST.ASTNode;
import cop5556fa20.AST.ASTVisitor;
import cop5556fa20.AST.DecImage;
import cop5556fa20.AST.DecVar;
import cop5556fa20.AST.ExprArg;
import cop5556fa20.AST.ExprBinary;
import cop5556fa20.AST.ExprConditional;
import cop5556fa20.AST.ExprConst;
import cop5556fa20.AST.ExprEmpty;
import cop5556fa20.AST.ExprHash;
import cop5556fa20.AST.ExprIntLit;
import cop5556fa20.AST.ExprPixelConstructor;
import cop5556fa20.AST.ExprPixelSelector;
import cop5556fa20.AST.ExprStringLit;
import cop5556fa20.AST.ExprUnary;
import cop5556fa20.AST.ExprVar;
import cop5556fa20.AST.Expression;
import cop5556fa20.AST.Program;
import cop5556fa20.AST.StatementAssign;
import cop5556fa20.AST.StatementImageIn;
import cop5556fa20.AST.StatementLoop;
import cop5556fa20.AST.StatementOutFile;
import cop5556fa20.AST.StatementOutScreen;
import cop5556fa20.AST.Type;
import cop5556fa20.runtime.LoggedIO;

public class CodeGen5 implements ASTVisitor, Opcodes {
	private static final String STRING_DESC = "Ljava/lang/String;";
	private static final String INTEGER_DESC = "I";

	final String className;
	final boolean isInterface = false;
	ClassWriter cw;
	MethodVisitor mv;
	
	public CodeGen5(String className) {
		super();
		this.className = className;
	}
	
	
	@Override
	public Object visitDecImage(DecImage decImage, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Add a static field to the class for this variable.
	 */
	@Override
	public Object visitDecVar(DecVar decVar, Object arg) throws Exception {
		String varName = decVar.name();
		Type type = decVar.type();

		String desc = STRING_DESC;
		Object defaultValue = null;
		if (type == Type.Int) {
			desc = INTEGER_DESC;
			defaultValue = 0;
		}

		FieldVisitor fieldVisitor = cw.visitField(ACC_STATIC, varName, desc, null, defaultValue);
		fieldVisitor.visitEnd();

		//evaluate initial value and store in variable, if one is given.
		Expression e = decVar.expression();
		if (e != Expression.empty) {
			e.visit(this, null); // generates code to evaluate expression and leave value on top of the stack
			mv.visitFieldInsn(PUTSTATIC, className, varName, desc);
		}

		return null;
	}

	@Override
	public Object visitExprArg(ExprArg exprArg, Object arg) throws Exception {
		// Loading local variable args
		mv.visitVarInsn(ALOAD, 0);
		// Load the index
		exprArg.e().visit(this, null);
		// Load the corresponding element
		mv.visitInsn(AALOAD);

		if (exprArg.type() == Type.Int) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
		}

		return null;
	}

	@Override
	public Object visitExprBinary(ExprBinary exprBinary, Object arg) throws Exception {
		exprBinary.e0().visit(this, null);
		exprBinary.e1().visit(this, null);

		Scanner.Kind op = exprBinary.op();
		switch (op) {
			case AND ->
					mv.visitInsn(IAND);
			case OR ->
					mv.visitInsn(IOR);
			case EQ -> {
				if (exprBinary.e0().type() == Type.Int) {
					addBooleanJumpLogic(IF_ICMPEQ);
				} else {
					addBooleanJumpLogic(IF_ACMPEQ);
				}
			}
			case NEQ -> {
				if (exprBinary.e0().type() == Type.Int) {
					addBooleanJumpLogic(IF_ICMPNE);
				} else {
					addBooleanJumpLogic(IF_ACMPNE);
				}
			}
			case LT ->
					addBooleanJumpLogic(IF_ICMPLT);
			case GT ->
					addBooleanJumpLogic(IF_ICMPGT);
			case LE ->
					addBooleanJumpLogic(IF_ICMPLE);
			case GE ->
					addBooleanJumpLogic(IF_ICMPGE);
			case PLUS -> {
				if (exprBinary.e0().type() == Type.Int) {
					mv.visitInsn(IADD);
				} else {
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", isInterface);
				}
			}
			case MINUS ->
					mv.visitInsn(ISUB);
			case STAR ->
					mv.visitInsn(IMUL);
			case DIV ->
					mv.visitInsn(IDIV);
			case MOD ->
					mv.visitInsn(IREM);
			default ->
					throw new UnsupportedOperationException("Operator " + op + " not supported in binary expression");
		}
		return null;
	}

	@Override
	public Object visitExprConditional(ExprConditional exprConditional, Object arg) throws Exception {
		exprConditional.condition().visit(this, null);

		Label falseLabel = new Label();
		Label endLabel = new Label();

		mv.visitJumpInsn(IFEQ, falseLabel);

		exprConditional.trueCase().visit(this, null);
		mv.visitJumpInsn(GOTO, endLabel);

		mv.visitLabel(falseLabel);
		exprConditional.falseCase().visit(this, null);

		mv.visitLabel(endLabel);

		return null;
	}

	@Override
	public Object visitExprConst(ExprConst exprConst, Object arg) throws Exception {
		mv.visitLdcInsn(exprConst.value());
		return null;
	}

	@Override
	public Object visitExprHash(ExprHash exprHash, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Object visitExprIntLit(ExprIntLit exprIntLit, Object arg) throws Exception {
		mv.visitLdcInsn(exprIntLit.value());
		return null;
	}

	@Override
	public Object visitExprPixelConstructor(ExprPixelConstructor exprPixelConstructor, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not yet implemented");
	}
	@Override
	public Object visitExprPixelSelector(ExprPixelSelector exprPixelSelector, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * generate code to put the value of the StringLit on the stack.
	 */
	@Override
	public Object visitExprStringLit(ExprStringLit exprStringLit, Object arg) throws Exception {
		mv.visitLdcInsn(exprStringLit.text());
		return null;
	}

	@Override
	public Object visitExprUnary(ExprUnary exprUnary, Object arg) throws Exception {
		Scanner.Kind op = exprUnary.op();

		if (isKind(op, Scanner.Kind.PLUS, Scanner.Kind.MINUS)) {
			exprUnary.e().visit(this, Type.Int);
			if (isKind(op, Scanner.Kind.MINUS)) {
				mv.visitInsn(INEG);
			}
		}

		if (isKind(op, Scanner.Kind.EXCL)) {
			exprUnary.e().visit(this, null);
			addBooleanJumpLogic(IFEQ);
		}

		return null;
	}

	@Override
	public Object visitExprVar(ExprVar exprVar, Object arg) throws Exception {
		String name = exprVar.name();

		if (exprVar.type() == Type.Int) {
			mv.visitFieldInsn(GETSTATIC, className, name, INTEGER_DESC);
		}

		if (exprVar.type() == Type.String) {
			mv.visitFieldInsn(GETSTATIC, className, name, STRING_DESC);
		}

		return null;
	}
	
	
	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
//		cw = new ClassWriter(0); //If the call to methodVisitor.visitMaxs crashes, it
		// is
		// sometime helpful to
		// temporarily run it without COMPUTE_FRAMES. You
		// won't get a completely correct classfile, but
		// you will be able to see the code that was
		// generated.

		// String sourceFileName = className; //TODO Temporary solution, FIX THIS
		int version = -65478;
		cw.visit(version, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", null);
		cw.visitSource(null, null);
		// create main method
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		// initialize
		mv.visitCode();
		// insert label before first instruction
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// visit children to add instructions to method
		List<ASTNode> nodes = program.decOrStatement();
		for (ASTNode node : nodes) {
			node.visit(this, null);
		}
		// add  required (by the JVM) return statement to main
		mv.visitInsn(RETURN);

		// adds label at end of code
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		// handles parameters and local variables of main. The only local var is args
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		// Sets max stack size and number of local vars.
		// Because we use ClassWriter.COMPUTE_FRAMES as a parameter in the constructor,
		// asm will calculate this itself and the parameters are ignored.
		// If you have trouble with failures in this routine, it may be useful
		// to temporarily set the parameter in the ClassWriter constructor to 0.
		// The generated classfile will not pass verification, but you will at least be
		// able to see what instructions it contains.
		mv.visitMaxs(0, 0);

		// finish construction of main method
		mv.visitEnd();

		// finish class construction
		cw.visitEnd();

		// generate classfile as byte array and return
		return cw.toByteArray();
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws Exception {
		String name = statementAssign.name();
		statementAssign.expression().visit(this, null);

		switch (statementAssign.dec().type()) {
			case Int ->
					mv.visitFieldInsn(PUTSTATIC, className, name, INTEGER_DESC);
			case String ->
					mv.visitFieldInsn(PUTSTATIC, className, name, STRING_DESC);
		}
		return null;
	}
	@Override
	public Object visitStatementImageIn(StatementImageIn statementImageIn, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not yet implemented");
	}
	@Override
	public Object visitStatementLoop(StatementLoop statementLoop, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Object visitExprEmpty(ExprEmpty exprEmpty, Object arg) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStatementOutFile(StatementOutFile statementOutFile, Object arg) throws Exception {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Object visitStatementOutScreen(StatementOutScreen statementOutScreen, Object arg) throws Exception {
		String name = statementOutScreen.name();

		Type type = statementOutScreen.dec().type();
		switch (type) {
			case String -> {
				mv.visitFieldInsn(GETSTATIC, className, name, STRING_DESC);
				mv.visitMethodInsn(INVOKESTATIC, LoggedIO.className, "stringToScreen", LoggedIO.stringToScreenSig,
						isInterface);
			}
			case Int -> {
				mv.visitFieldInsn(GETSTATIC, className, name, INTEGER_DESC);
				mv.visitMethodInsn(INVOKESTATIC, LoggedIO.className, "intToScreen", LoggedIO.intToScreenSig,
						isInterface);
			}
			default ->
				throw new UnsupportedOperationException("not yet implemented");
		}
		return null;
	}

	private void addBooleanJumpLogic(int opCode) {
		Label trueLabel = new Label();
		Label endLabel = new Label();

		mv.visitJumpInsn(opCode, trueLabel);
		mv.visitInsn(ICONST_0);
		mv.visitJumpInsn(GOTO, endLabel);
		mv.visitLabel(trueLabel);
		mv.visitInsn(ICONST_1);
		mv.visitLabel(endLabel);
	}

	private boolean isKind(Scanner.Kind actual, Scanner.Kind...expectedKinds) {
		for (Scanner.Kind expectedKind : expectedKinds) {
			if (actual == expectedKind) {
				return true;
			}
		}
		return false;
	}
}
