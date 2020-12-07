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

import cop5556fa20.AST.Type;
import cop5556fa20.AST.*;
import cop5556fa20.runtime.LoggedIO;
import cop5556fa20.runtime.PLPImage;
import cop5556fa20.runtime.PLPImageUtils;
import cop5556fa20.runtime.PixelOps;
import org.objectweb.asm.*;

import java.util.List;

public class CodeGenVisitorComplete implements ASTVisitor, Opcodes {
	private static final String STRING_DESC = "Ljava/lang/String;";
	private static final String INTEGER_DESC = "Ljava/lang/Integer;";
	private static final String INT_DESC = "I";

	private static final String INTEGER_CLASS_NAME = "java/lang/Integer";
	final String className;
	final boolean isInterface = false;
	ClassWriter cw;
	MethodVisitor mv;

	public CodeGenVisitorComplete(String className) {
		super();
		this.className = className;
	}
	
	
	@Override
	public Object visitDecImage(DecImage decImage, Object arg) throws Exception {
		// create the static variable
		FieldVisitor fieldVisitor = cw.visitField(ACC_STATIC, decImage.name(), PLPImage.desc, null, null);
		fieldVisitor.visitEnd();

		// Load operator
		mv.visitFieldInsn(GETSTATIC, Scanner.KIND_CLASS_NAME, decImage.op().toString(), Scanner.KIND_DESC);

		// Load width
		if (decImage.width() == Expression.empty) {
			mv.visitInsn(ACONST_NULL);
		} else {
			decImage.width().visit(this, null);
			mv.visitMethodInsn(INVOKESTATIC, INTEGER_CLASS_NAME, "valueOf", "(I)"+INTEGER_DESC, false);
		}

		// Load height
		if (decImage.height() == Expression.empty) {
			mv.visitInsn(ACONST_NULL);
		} else {
			decImage.height().visit(this, null);
			mv.visitMethodInsn(INVOKESTATIC, INTEGER_CLASS_NAME, "valueOf", "(I)"+INTEGER_DESC, false);
		}

		// Load source
		if (decImage.source() == Expression.empty) {
			mv.visitInsn(ACONST_NULL);
		} else {
			decImage.source().visit(this, null);
		}

		// Load line number
		mv.visitLdcInsn(decImage.first().line());

		// Load pos in line
		mv.visitLdcInsn(decImage.first().posInLine());

		// Call the java method which will create the image
		mv.visitMethodInsn(INVOKESTATIC, PLPImageUtils.className, "createImage", PLPImageUtils.createImageSig, false);

		// put the value of the static variable
		mv.visitFieldInsn(PUTSTATIC, className, decImage.name(), PLPImage.desc);

		return null;
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
			desc = INT_DESC;
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
		Type eType = exprHash.e().type();
		exprHash.e().visit(this, null);
		if (eType == Type.Int) {
			if (exprHash.attr().equals("red")) {
				mv.visitMethodInsn(INVOKESTATIC, PixelOps.className, "getRed", PixelOps.getRedSig, false);
			}
			if (exprHash.attr().equals("green")) {
				mv.visitMethodInsn(INVOKESTATIC, PixelOps.className, "getGreen", PixelOps.getGreenSig, false);
			}
			if (exprHash.attr().equals("blue")) {
				mv.visitMethodInsn(INVOKESTATIC, PixelOps.className, "getBlue", PixelOps.getBlueSig, false);
			}
		}
		if (eType == Type.Image) {
			// Load line number
			mv.visitLdcInsn(exprHash.first().line());

			// Load pos in line
			mv.visitLdcInsn(exprHash.first().posInLine());

			if (exprHash.attr().equals("width")) {
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getWidthThrows", PLPImage.getWidthThrowsSig, false);
			} else if (exprHash.attr().equals("height")) {
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getHeightThrows", PLPImage.getHeightThrowsSig, false);
			}

		}
		return null;
	}

	@Override
	public Object visitExprIntLit(ExprIntLit exprIntLit, Object arg) throws Exception {
		mv.visitLdcInsn(exprIntLit.value());
		return null;
	}

	@Override
	public Object visitExprPixelConstructor(ExprPixelConstructor exprPixelConstructor, Object arg) throws Exception {
		exprPixelConstructor.redExpr().visit(this, null);
		exprPixelConstructor.greenExpr().visit(this, null);
		exprPixelConstructor.blueExpr().visit(this, null);

		mv.visitMethodInsn(INVOKESTATIC, PixelOps.className, "makePixel", PixelOps.makePixelSig, false);
		return null;
	}

	@Override
	public Object visitExprPixelSelector(ExprPixelSelector exprPixelSelector, Object arg) throws Exception {
		exprPixelSelector.image().visit(this, null);
		exprPixelSelector.X().visit(this, null);
		exprPixelSelector.Y().visit(this, null);
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "selectPixel", PLPImage.selectPixelSig, false);
		return null;
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
		switch (exprVar.type()) {
			case Int -> {
				if (name.equals("X")) {
					mv.visitVarInsn(ILOAD, 1);
				} else if (name.equals("Y")) {
					mv.visitVarInsn(ILOAD, 2);
				} else {
					mv.visitFieldInsn(GETSTATIC, className, name, INT_DESC);
				}
			}

			case String -> mv.visitFieldInsn(GETSTATIC, className, name, STRING_DESC);
			case Image -> mv.visitFieldInsn(GETSTATIC, className, name, PLPImage.desc);
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
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, new String[] { "java/lang/Exception" });
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
		mv.visitLocalVariable("X", INT_DESC, null, mainStart, mainEnd, 1);
		mv.visitLocalVariable("Y", INT_DESC, null, mainStart, mainEnd, 2);
		mv.visitLocalVariable("width", INT_DESC, null, mainStart, mainEnd, 3);
		mv.visitLocalVariable("height", INT_DESC, null, mainStart, mainEnd, 4);

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
					mv.visitFieldInsn(PUTSTATIC, className, name, INT_DESC);
			case String ->
					mv.visitFieldInsn(PUTSTATIC, className, name, STRING_DESC);
			case Image -> {
				// Load the LHS image
				mv.visitFieldInsn(GETSTATIC, className, name, PLPImage.desc);

				// Load the RHS image
				statementAssign.expression().visit(this, null);

				// Load line number
				mv.visitLdcInsn(statementAssign.first().line());

				// Load pos in line
				mv.visitLdcInsn(statementAssign.first().posInLine());

				// Call the java method which will assign the image
				mv.visitMethodInsn(INVOKESTATIC, PLPImageUtils.className, "assignImage", PLPImageUtils.assignImageSig, false);

				// put the value of the static variable
				// mv.visitFieldInsn(PUTSTATIC, className, name, PLPImage.desc);
			}

		}
		return null;
	}

	@Override
	public Object visitStatementImageIn(StatementImageIn statementImageIn, Object arg) throws Exception {

		// Load the image
		mv.visitFieldInsn(GETSTATIC, className, statementImageIn.name(), PLPImage.desc);

		// Load the source
		statementImageIn.source().visit(this, null);

		// Call the java method which will copy the image
		mv.visitMethodInsn(INVOKESTATIC, PLPImageUtils.className, "copyImage", PLPImageUtils.copyImageSig, false);

		// put the value of the static variable
		// mv.visitFieldInsn(PUTSTATIC, className, statementImageIn.name(), PLPImage.desc);

		return null;
	}

	@Override
	public Object visitStatementLoop(StatementLoop statementLoop, Object arg) throws Exception {

		// Ensure Image is allocated
		mv.visitFieldInsn(GETSTATIC, className, statementLoop.name(), PLPImage.desc);
		mv.visitLdcInsn(statementLoop.first().line());
		mv.visitLdcInsn(statementLoop.first().posInLine());
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "ensureImageAllocated", PLPImage.ensureImageAllocatedSig, isInterface);

		// Getting width of the image
		mv.visitFieldInsn(GETSTATIC, className, statementLoop.name(), PLPImage.desc);
		mv.visitLdcInsn(statementLoop.first().line());
		mv.visitLdcInsn(statementLoop.first().posInLine());
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getWidthThrows", PLPImage.getWidthThrowsSig, isInterface);
		mv.visitVarInsn(ISTORE, 3);

		// Getting height of the image
		mv.visitFieldInsn(GETSTATIC, className, statementLoop.name(), PLPImage.desc);
		mv.visitLdcInsn(statementLoop.first().line());
		mv.visitLdcInsn(statementLoop.first().posInLine());
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "getHeightThrows", PLPImage.getHeightThrowsSig, isInterface);
		mv.visitVarInsn(ISTORE, 4);

		// All labels
		Label outerLoop = new Label();
		Label endOuterForLoop = new Label();

		Label innerLoop = new Label();
		Label endInnerLoop = new Label();

		{
			// Initialization: int x = 0;
			{
				mv.visitInsn(ICONST_0);
				mv.visitVarInsn(ISTORE, 1);
			}
			// Outer loop
			mv.visitLabel(outerLoop);
			{
				// Load x
				mv.visitVarInsn(ILOAD, 1);
				// Load width
				mv.visitVarInsn(ILOAD, 3);
				// condition check
				mv.visitJumpInsn(IF_ICMPGE, endOuterForLoop);
				{
					// Initialization: int y = 0;
					{
						mv.visitInsn(ICONST_0);
						mv.visitVarInsn(ISTORE, 2);
					}
					// Inner loop
					mv.visitLabel(innerLoop);
					{
						// Load y
						mv.visitVarInsn(ILOAD, 2);
						// Load height
						mv.visitVarInsn(ILOAD, 4);
						// condition check
						mv.visitJumpInsn(IF_ICMPGE, endInnerLoop);
						{
							if (statementLoop.cond() == Expression.empty) {
								executeLoopStatement(statementLoop.name(), statementLoop.e());
							} else {
								Label endStatement = new Label();
								statementLoop.cond().visit(this, null);
								mv.visitJumpInsn(IFEQ, endStatement);
								executeLoopStatement(statementLoop.name(), statementLoop.e());
								mv.visitLabel(endStatement);
							}
						}
					}

					// Post loop: Increment y
					mv.visitInsn(ICONST_1);
					mv.visitVarInsn(ILOAD, 2);
					mv.visitInsn(IADD);
					mv.visitVarInsn(ISTORE, 2);

					// Again run the loop
					mv.visitJumpInsn(GOTO, innerLoop);

				}
				mv.visitLabel(endInnerLoop);
			}


			// Post loop: Increment x
			mv.visitInsn(ICONST_1);
			mv.visitVarInsn(ILOAD, 1);
			mv.visitInsn(IADD);
			mv.visitVarInsn(ISTORE, 1);

			// Again run the loop
			mv.visitJumpInsn(GOTO, outerLoop);
		}
		mv.visitLabel(endOuterForLoop);

		return null;
	}

	private void executeLoopStatement(String name, Expression e) throws Exception {
		mv.visitFieldInsn(GETSTATIC, className, name, PLPImage.desc);
		mv.visitVarInsn(ILOAD, 1);
		mv.visitVarInsn(ILOAD, 2);
		e.visit(this, null);
		mv.visitMethodInsn(INVOKEVIRTUAL, PLPImage.className, "updatePixel", PLPImage.updatePixelSig, isInterface);
	}

	@Override
	public Object visitExprEmpty(ExprEmpty exprEmpty, Object arg) throws Exception {
		return null;
	}

	@Override
	public Object visitStatementOutFile(StatementOutFile statementOutFile, Object arg) throws Exception {
		// Load the image
		mv.visitFieldInsn(GETSTATIC, className, statementOutFile.name(), PLPImage.desc);

		// Load the file name
		statementOutFile.filename().visit(this, null);

		// Call the LoggedIO.imageToFile method
		mv.visitMethodInsn(INVOKESTATIC, LoggedIO.className, "imageToFile", LoggedIO.imageToFileSig, isInterface);

		return null;
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
				mv.visitFieldInsn(GETSTATIC, className, name, INT_DESC);
				mv.visitMethodInsn(INVOKESTATIC, LoggedIO.className, "intToScreen", LoggedIO.intToScreenSig,
						isInterface);
			}
			case Image -> {
				// Load the image
				mv.visitFieldInsn(GETSTATIC, className, statementOutScreen.name(), PLPImage.desc);

				// Load the xloc
				if (statementOutScreen.X() == Expression.empty) {
					mv.visitInsn(ICONST_0);
				} else {
					statementOutScreen.X().visit(this, null);
				}

				// Load the yloc
				if (statementOutScreen.Y() == Expression.empty) {
					mv.visitInsn(ICONST_0);
				} else {
					statementOutScreen.Y().visit(this, null);
				}

				// Call the LoggedIO.imageToScreen method
				mv.visitMethodInsn(INVOKESTATIC, LoggedIO.className, "imageToScreen", LoggedIO.imageToScreenSig,
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
