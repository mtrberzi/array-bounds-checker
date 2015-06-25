package org.checkerframework.checker.arraysafety;

import org.checkerframework.checker.arraysafety.qual.*;

import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.ArrayLen;

import java.util.List;
import org.checkerframework.javacutil.Pair;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;

public class ArraySafetyAnnotatedTypeFactory extends GenericAnnotatedTypeFactory<CFValue, CFStore, ArraySafetyTransfer, ArraySafetyAnalysis> {

    protected final AnnotationMirror UNSAFE_ARRAY_INDEX;
    protected final AnnotationMirror INTVAL;
    protected final AnnotationMirror ARRAYLEN;
    
    public ArraySafetyAnnotatedTypeFactory(BaseTypeChecker checker) {
	super(checker);

	UNSAFE_ARRAY_INDEX = AnnotationUtils.fromClass(elements, UnsafeArrayIndex.class);
	INTVAL = AnnotationUtils.fromClass(elements, IntVal.class);
	ARRAYLEN = AnnotationUtils.fromClass(elements, ArrayLen.class);
	
	this.postInit();
    }
    
    @Override
    protected ArraySafetyAnalysis createFlowAnalysis(List<Pair<VariableElement, CFValue>> fieldValues) {
	return new ArraySafetyAnalysis(checker, this, fieldValues);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
	return new ListTreeAnnotator(
				     new ImplicitsTreeAnnotator(this),
				     new ArraySafetyTreeAnnotator(this)
				     );
    }

    AnnotationMirror createUnsafeArrayIndexAnnotation() {
	AnnotationBuilder builder = new AnnotationBuilder(processingEnv, UnsafeArrayIndex.class);
	return builder.build();
    }
   
    private class ArraySafetyTreeAnnotator extends TreeAnnotator {
	public ArraySafetyTreeAnnotator(AnnotatedTypeFactory aTypeFactory) {
	    super(aTypeFactory);
	}

	public List<Long> getIntValues(AnnotatedTypeMirror type) {
	    AnnotationMirror intAnno = type.getAnnotationInHierarchy(INTVAL);
	    return AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, true);
	}

	// Annotate negative integer constants with @UnsafeArrayIndex.
	@Override
	public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
	    if (!type.isAnnotatedInHierarchy(UNSAFE_ARRAY_INDEX)) {
		if (tree.getKind() == Tree.Kind.INT_LITERAL) {
		    Integer lit = (Integer)tree.getValue();
		    if (lit < 0) {
			type.addAnnotation(createUnsafeArrayIndexAnnotation());
		    }
		}
	    }

	    GenericAnnotatedTypeFactory<?, ?, ?, ?> valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
	    assert valueATF != null : "cannot access ValueChecker annotations";
	    AnnotatedTypeMirror valueType = valueATF.getAnnotatedType(tree);
	    if (valueType.hasAnnotation(IntVal.class)) {
		List<Long> intValues = getIntValues(valueType);
		for (Long value : intValues) {
		    if (value < 0L) {
			type.addAnnotation(createUnsafeArrayIndexAnnotation());
		    }
		}
	    }
	    
	    return super.visitLiteral(tree, type);
	}

	@Override
	public Void visitArrayAccess(ArrayAccessTree tree, AnnotatedTypeMirror type) {
	    GenericAnnotatedTypeFactory<?, ?, ?, ?> valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
	    assert valueATF != null : "cannot access ValueChecker annotations";

	    ExpressionTree arrayTree = tree.getExpression();
	    AnnotatedTypeMirror arrayType = valueATF.getAnnotatedType(arrayTree);

	    ExpressionTree indexTree = tree.getIndex();
	    AnnotatedTypeMirror indexType = valueATF.getAnnotatedType(indexTree);

	    if (arrayType.hasAnnotation(ArrayLen.class) && indexType.hasAnnotation(IntVal.class)) {
		// TODO bounds check
		//type.addAnnotation(createUnsafeArrayIndexAnnotation());
	    }
	    
	    return super.visitArrayAccess(tree, type);
	}
	
    }
    
}
