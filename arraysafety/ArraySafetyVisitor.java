package org.checkerframework.checker.arraysafety;

import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.arraysafety.qual.*;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.source.Result;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.IntVal;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ArrayAccessTree;

public class ArraySafetyVisitor extends BaseTypeVisitor<ArraySafetyAnnotatedTypeFactory> {

    public ArraySafetyVisitor(BaseTypeChecker checker) {
	super(checker);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
	AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(node);
	
	if (exprType.hasAnnotation(UnsafeArrayAccess.class)) {
	    checker.report(Result.failure("array.access.unsafe"), node);
	}

	// TODO check for *safe* accesses
	
	return super.visitArrayAccess(node, p);
    }
    
}
