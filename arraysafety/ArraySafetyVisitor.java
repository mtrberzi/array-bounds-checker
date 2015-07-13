package org.checkerframework.checker.arraysafety;

import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.arraysafety.qual.*;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.source.Result;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ArrayAccessTree;

import javax.lang.model.element.AnnotationMirror;

public class ArraySafetyVisitor extends BaseTypeVisitor<ArraySafetyAnnotatedTypeFactory> {

    protected final AnnotationMirror UNBOUNDED;
    
    public ArraySafetyVisitor(BaseTypeChecker checker) {
	super(checker);

	UNBOUNDED = AnnotationUtils.fromClass(elements, Unbounded.class);
    }

    public Integer getLowerBound(AnnotatedTypeMirror type) {
	AnnotationMirror boundedAnno = type.getAnnotationInHierarchy(UNBOUNDED);
	return AnnotationUtils.getElementValue(boundedAnno, "lowerBound", Integer.class, true);
    }
    
    public Integer getUpperBound(AnnotatedTypeMirror type) {
	AnnotationMirror boundedAnno = type.getAnnotationInHierarchy(UNBOUNDED);
	return AnnotationUtils.getElementValue(boundedAnno, "upperBound", Integer.class, true);
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
	    System.out.println("--- unbounded index");
	} else if (arrayType.hasAnnotation(Bounded.class) && indexType.hasAnnotation(Bounded.class)) {
	    
	    Integer arrayLower = getLowerBound(arrayType);
	    Integer arrayUpper = getUpperBound(arrayType);
	    Integer indexLower = getLowerBound(indexType);
	    Integer indexUpper = getUpperBound(indexType);

	    if (indexLower < 0) {
		checker.report(Result.failure("array.access.unsafe"), node);
		System.out.println("--- index can be negative");
	    } else if (indexUpper >= arrayLower) {
		// unsafe because the index might be larger than the shortest possible array
		checker.report(Result.failure("array.access.unsafe"), node);
		System.out.println("--- index can be out of bounds");
	    } else {
		System.out.println("+++ ok");
	    }
	    
	} else if (arrayType.hasAnnotation(Unbounded.class)) {
	    // counts as a warning because we can't prove whether the access is safe
	    checker.report(Result.warning("array.access.unknown"), node);
	    System.out.println("--- unbounded array");
	}

	return super.visitArrayAccess(node, p);
    }
    
}
