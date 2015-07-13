package org.checkerframework.checker.arraysafety;

import org.checkerframework.checker.arraysafety.qual.*;

import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
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

import com.sun.source.tree.*;

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

	@Override
	public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
	    Tree.Kind kind = tree.getKind();
	    if (kind.equals(Tree.Kind.INT_LITERAL)) {
		Integer literal = (Integer)tree.getValue();
		type.replaceAnnotation(createBoundedAnnotation(literal, literal));
	    }
	    //return super.visitLiteral(tree, type);
	    return null;
	}
	
	@Override
	public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
	    GenericAnnotatedTypeFactory<?, ?, ?, ?> valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
	    assert valueATF != null : "cannot access ValueChecker annotations";
	    if (tree.getDimensions().size() > 1) {
		throw new UnsupportedOperationException("cannot handle multidimensional arrays");
	    }
	    ExpressionTree dimension = tree.getDimensions().get(0);
	    AnnotatedTypeMirror dimensionValueType = valueATF.getAnnotatedType(dimension);
	    AnnotatedTypeMirror dimensionBoundsType = atypeFactory.getAnnotatedType(dimension);
	    if (dimensionValueType.hasAnnotation(IntVal.class)) {
		List<Long> indexValues = getIntValues(dimensionValueType);
		// TODO check if the index could be negative
		type.replaceAnnotation(createBoundedAnnotationFromIntVal(dimensionValueType));
	    } else if (dimensionBoundsType.hasAnnotation(Bounded.class)) {
		Integer lowerBound = getLowerBound(dimensionBoundsType);
		Integer upperBound = getUpperBound(dimensionBoundsType);
		type.replaceAnnotation(createBoundedAnnotation(lowerBound, upperBound));
	    } else if (dimensionBoundsType.hasAnnotation(Unbounded.class)) {
		type.replaceAnnotation(UNBOUNDED);
	    }
	    //return super.visitNewArray(tree, type);
	    return null;
	}
		
	public AnnotationMirror createBoundedAnnotationFromIntVal(AnnotatedTypeMirror intValType) {
	    List<Long> indexValues = getIntValues(intValType);
	    // the minimum value in this list is the lower bound;
	    // the maximum value in this list is the upper bound
	    Integer lowerBound = indexValues.get(0).intValue();
	    Integer upperBound = indexValues.get(0).intValue();
	    for (int i = 1; i < indexValues.size(); ++i) {
		Integer x = indexValues.get(i).intValue();
		if (x < lowerBound) {
		    lowerBound = x;
		}
		if (x > upperBound) {
		    upperBound = x;
		}
	    }
	    return createBoundedAnnotation(lowerBound, upperBound);
	}
	
	public Integer getLowerBound(AnnotatedTypeMirror type) {
	    AnnotationMirror boundedAnno = type.getAnnotationInHierarchy(UNBOUNDED);
	    return AnnotationUtils.getElementValue(boundedAnno, "lowerBound", Integer.class, true);
	}
	
	public Integer getUpperBound(AnnotatedTypeMirror type) {
	    AnnotationMirror boundedAnno = type.getAnnotationInHierarchy(UNBOUNDED);
	    return AnnotationUtils.getElementValue(boundedAnno, "upperBound", Integer.class, true);
	}
	
	public List<Long> getIntValues(AnnotatedTypeMirror type) {
	    AnnotationMirror intAnno = type.getAnnotationInHierarchy(INTVAL);
	    return AnnotationUtils.getElementValueArray(intAnno, "value", Long.class, true);
	}

	public List<Integer> getArrayLengths(AnnotatedTypeMirror type) {
	    AnnotationMirror arrAnno = type.getAnnotationInHierarchy(ARRAYLEN);
	    return AnnotationUtils.getElementValueArray(arrAnno, "value", Integer.class, true);
	}
	/*
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
         * structure unless both annotations are Bounded.
	 * In this case, rhs is a subtype of lhs
	 * iff lhs contains at least every element of rhs
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

		return (Llhs <= Lrhs) && (Urhs <= Ulhs);
		
	    } else {
		return false;
	    }
	}
	
    }
    
}
