package org.checkerframework.checker.arraysafety;

import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.arraysafety.qual.*;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.source.Result;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ArrayAccessTree;

public class ArraySafetyVisitor extends BaseTypeVisitor<ArraySafetyAnnotatedTypeFactory> {

    public ArraySafetyVisitor(BaseTypeChecker checker) {
	super(checker);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
	ExpressionTree array = node.getExpression();
	ExpressionTree index = node.getIndex();

	AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(index);
	if (indexType.hasAnnotation(UnsafeArrayIndex.class)) {
	    checker.report(Result.failure("array.access.unsafe"), node);
	}
	
	return super.visitArrayAccess(node, p);
    }
    
}
