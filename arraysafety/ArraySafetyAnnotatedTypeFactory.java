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
import org.checkerframework.framework.source.Result;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.type.QualifierHierarchy;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import org.checkerframework.javacutil.Pair;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;

public class ArraySafetyAnnotatedTypeFactory extends GenericAnnotatedTypeFactory<CFValue, CFStore, ArraySafetyTransfer, ArraySafetyAnalysis> {

    protected final AnnotationMirror INTVAL;
    protected final AnnotationMirror ARRAYLEN;

    protected final AnnotationMirror UNBOUNDED;
    protected final AnnotationMirror BOUNDSBOTTOM;
    
    public ArraySafetyAnnotatedTypeFactory(BaseTypeChecker checker) {
	super(checker);

	UNBOUNDED = AnnotationUtils.fromClass(elements, Unbounded.class);
	BOUNDSBOTTOM = AnnotationUtils.fromClass(elements, BoundsBottom.class);
	
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

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
	return new ArraySafetyQualifierHierarchy(factory);
    }

    @Override
    protected QualifierDefaults createQualifierDefaults() {
	QualifierDefaults defaults = super.createQualifierDefaults();
	defaults.addAbsoluteDefault(UNBOUNDED, DefaultLocation.OTHERWISE);
	defaults.addAbsoluteDefault(BOUNDSBOTTOM, DefaultLocation.LOWER_BOUNDS);

	return defaults;
    }

    /*
    AnnotationMirror createUnsafeArrayAccessAnnotation() {
	return createUnsafeArrayAccessAnnotation(new HashSet<String>());
    }

    AnnotationMirror createUnsafeArrayAccessAnnotation(Set<String> values) {
	AnnotationBuilder builder = new AnnotationBuilder(processingEnv, UnsafeArrayAccess.class);
	List<String> valuesList = new ArrayList<String>(values);
	builder.setValue("value", valuesList);
	return builder.build();
    }

    AnnotationMirror createSafeArrayAccessAnnotation() {
	return createSafeArrayAccessAnnotation(new HashSet<String>());
    }
    
    AnnotationMirror createSafeArrayAccessAnnotation(Set<String> values) {
	AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SafeArrayAccess.class);
	List<String> valuesList = new ArrayList<String>(values);
	builder.setValue("value", valuesList);
	return builder.build();
    }
    */

    AnnotationMirror createBoundedAnnotation(Integer lowerBound, Integer upperBound) {
	AnnotationBuilder builder = new AnnotationBuilder(processingEnv, Bounded.class);
	builder.setValue("lowerBound", lowerBound);
	builder.setValue("upperBound", upperBound);
	return builder.build();
    }
    
    private class ArraySafetyTreeAnnotator extends TreeAnnotator {
	public ArraySafetyTreeAnnotator(AnnotatedTypeFactory aTypeFactory) {
	    super(aTypeFactory);
	}
	/*
	public List<Long> getIntValues(AnnotatedTypeMirror type) {
	    AnnotationMirror intAnno = type.getAnnotationInHierarchy(INTVAL);
	    return AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, true);
	}

	public List<Integer> getArrayLengths(AnnotatedTypeMirror type) {
	    AnnotationMirror arrAnno = type.getAnnotationInHierarchy(ARRAYLEN);
	    return AnnotationUtils.getElementValueArray(arrAnno, "value", Integer.class, true);
	}

	@Override
	public Void visitArrayAccess(ArrayAccessTree tree, AnnotatedTypeMirror type) {
	    if (!type.isAnnotatedInHierarchy(UNSAFE_ARRAY_ACCESS)true) {
		GenericAnnotatedTypeFactory<?, ?, ?, ?> valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
		assert valueATF != null : "cannot access ValueChecker annotations";
		
		ExpressionTree arrayTree = tree.getExpression();
		AnnotatedTypeMirror arrayType = valueATF.getAnnotatedType(arrayTree);

		ExpressionTree indexTree = tree.getIndex();
		AnnotatedTypeMirror indexType = valueATF.getAnnotatedType(indexTree);
		
		if (indexType.hasAnnotation(IntVal.class)) {
		    List<Long> indexValues = getIntValues(indexType);
		    if (arrayType.hasAnnotation(ArrayLen.class)) {
			List<Integer> arrayLengths = getArrayLengths(arrayType);
			boolean definitelyUnsafe = false;
			for (Long idx : indexValues) {
			    for (Integer i_len : arrayLengths) {
				Long len = new Long(i_len);
				if (idx >= len) {
				    definitelyUnsafe = true;
				    break;
				}
			    }
			}
			if (definitelyUnsafe) {
			    checker.report(Result.failure("array.access.unsafe"), tree);
			}
		    }
		    // if the index could possibly be negative, it is unsafe
		    for (Long idx : indexValues) {
		    	if (idx < 0) {
			    checker.report(Result.failure("array.access.unsafe"), tree);
		    	}
		    }
		}
	    }
	    
	    return super.visitArrayAccess(tree, type);
	}	
	*/
    }
	

    private final class ArraySafetyQualifierHierarchy extends MultiGraphQualifierHierarchy {

	public ArraySafetyQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
	    super(factory);
	}

	@Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1,
                AnnotationMirror a2) {
            if (isSubtype(a1, a2)) {
                return a1;
            } else if (isSubtype(a2, a1)) {
                return a2;
            } else {
                // If the two are unrelated, then bottom is the GLB.
                return BOUNDSBOTTOM;
            }
        }

	/**
	 * Determines the least upper bound of a1 and a2.
	 * If a1 and a2 are both Bounded, let a1's bounds be L1, U1
	 * and a2's bounds be L2, U2. Then the least upper bound
	 * is Bounded( min(L1, L2), max(U1, U2) ).
	 */
	@Override
	public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
	    if (!AnnotationUtils.areSameIgnoringValues(getTopAnnotation(a1), getTopAnnotation(a2))) {
		return null;
	    } else if (isSubtype(a1, a2)) {
		return a2;
	    } else if (isSubtype(a2, a1)) {
		return a1;
	    }
	    // if both are BOUNDED, take the "union" of the bounds
	    else if (AnnotationUtils.areSameByClass(a1, Bounded.class)
		     && AnnotationUtils.areSameByClass(a2, Bounded.class)) {
		Integer L1 = AnnotationUtils.getElementValue(a1, "lowerBound", Integer.class, true);
		Integer L2 = AnnotationUtils.getElementValue(a2, "lowerBound", Integer.class, true);
		Integer U1 = AnnotationUtils.getElementValue(a1, "upperBound", Integer.class, true);
		Integer U2 = AnnotationUtils.getElementValue(a2, "upperBound", Integer.class, true);

		Integer newLowerBound = Integer.min(L1, L2);
		Integer newUpperBound = Integer.max(U1, U2);
		
		return createBoundedAnnotation(newLowerBound, newUpperBound);
	    }
	    // annotations are in this hierarchy but not the same
	    else {
		// if either is UNBOUNDED then the least upper bound is UNBOUNDED
		if (AnnotationUtils.areSameByClass(a1, Unbounded.class)
		    || AnnotationUtils.areSameByClass(a2, Unbounded.class)) {
		    return UNBOUNDED;
		} else {
		    // we don't know what to do here
		    return UNBOUNDED;
		}
	    }
	}

	/**
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are Safe/UnsafeAccess. 
	 * In this case, rhs is a subtype of lhs
	 * iff rhs contains all the values in lhs's bounds.
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
	    if (AnnotationUtils.areSameByClass(lhs, Unbounded.class)
		|| AnnotationUtils.areSameByClass(rhs, BoundsBottom.class)) {
		return true;
	    } else if (AnnotationUtils.areSameByClass(rhs, Unbounded.class)
		       || AnnotationUtils.areSameByClass(lhs, BoundsBottom.class)) {
		return false;
	    } else if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
		// both are Bounded
		Integer Llhs = AnnotationUtils.getElementValue(lhs, "lowerBound", Integer.class, true);
		Integer Lrhs = AnnotationUtils.getElementValue(rhs, "lowerBound", Integer.class, true);
		Integer Ulhs = AnnotationUtils.getElementValue(lhs, "upperBound", Integer.class, true);
		Integer Urhs = AnnotationUtils.getElementValue(rhs, "upperBound", Integer.class, true);

		return ( (Llhs <= Lrhs) && (Urhs >= Ulhs) );
		
	    } else {
		return false;
	    }
	}
	
    }
    
}
