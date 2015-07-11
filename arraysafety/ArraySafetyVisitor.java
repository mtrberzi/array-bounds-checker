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
	ExpressionTree array = node.getExpression();
	ExpressionTree index = node.getIndex();

	AnnotatedTypeMirror arrayType = atypeFactory.getAnnotatedType(array);
	AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(index);

	System.out.println("*** " + arrayType.getAnnotations().toString());
	System.out.println("*** " + indexType.getAnnotations().toString());

	if (arrayType.hasAnnotation(Bounded.class) && indexType.hasAnnotation(Unbounded.class)) {
	    checker.report(Result.failure("array.access.unsafe"), node);
	} else if (arrayType.hasAnnotation(Bounded.class) && indexType.hasAnnotation(Bounded.class)) {
	    // TODO subtype test
	} else if (arrayType.hasAnnotation(Unbounded.class)) {
	    // counts as a warning because we can't prove whether the access is safe
	    checker.report(Result.warning("array.access.unknown"), node);
	}

	return super.visitArrayAccess(node, p);
    }
    
}
