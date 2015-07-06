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

    protected final AnnotationMirror UNSAFE_ARRAY_ACCESS;
    protected final AnnotationMirror INTVAL;
    protected final AnnotationMirror ARRAYLEN;

    protected final AnnotationMirror UNKNOWNSAFETY;
    protected final AnnotationMirror BOTTOMSAFETY;
    
    public ArraySafetyAnnotatedTypeFactory(BaseTypeChecker checker) {
	super(checker);

	UNKNOWNSAFETY = AnnotationUtils.fromClass(elements, UnknownArrayAccess.class);
	BOTTOMSAFETY = AnnotationUtils.fromClass(elements, ArrayAccessBottom.class);
	
	UNSAFE_ARRAY_ACCESS = AnnotationUtils.fromClass(elements, UnsafeArrayAccess.class);
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
	defaults.addAbsoluteDefault(UNKNOWNSAFETY, DefaultLocation.OTHERWISE);
	defaults.addAbsoluteDefault(BOTTOMSAFETY, DefaultLocation.LOWER_BOUNDS);

	return defaults;
    }
    
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

    private AnnotationMirror createAnnotation(String name, Set<String> values) {
	AnnotationBuilder builder = new AnnotationBuilder(processingEnv, name);
	List<String> valuesList = new ArrayList<String>(values);
	builder.setValue("value", valuesList);
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

	public List<Integer> getArrayLengths(AnnotatedTypeMirror type) {
	    AnnotationMirror arrAnno = type.getAnnotationInHierarchy(ARRAYLEN);
	    return AnnotationUtils.getElementValueArray(arrAnno, "value", Integer.class, true);
	}

	@Override
	public Void visitArrayAccess(ArrayAccessTree tree, AnnotatedTypeMirror type) {
	    if (/*!type.isAnnotatedInHierarchy(UNSAFE_ARRAY_ACCESS)*/true) {
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
                return BOTTOMSAFETY;
            }
        }

	/**
	 * Determines the least upper bound of a1 and a2.
	 * If a1 and a2 are the same type of ArraySafety annotation, the LUB
	 * is the result of taking all values from both a1 and a2 and removing all duplicates.
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
	    // if both are the same type, determine that type and then merge
	    else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
		List<String> a1Values = AnnotationUtils.getElementValueArray(a1, "value", String.class, true);
		List<String> a2Values = AnnotationUtils.getElementValueArray(a2, "value", String.class, true);
		HashSet<String> newValues = new HashSet<String>(a1Values.size() + a2Values.size());

		// if either of the original value sets are empty, that means "any"
		// which is always a strict upper bound
		if (!(a1Values.isEmpty()) && !(a2Values.isEmpty())) {
		    newValues.addAll(a1Values);
		    newValues.addAll(a2Values);
		}
		return createAnnotation(a1.getAnnotationType().toString(), newValues);
	    }
	    // annotations are in this hierarchy but not the same
	    else {
		// if either is UNKNOWNSAFETY then the LUB is UNKNOWNSAFETY
		if (AnnotationUtils.areSameByClass(a1, UnknownArrayAccess.class)
		    || AnnotationUtils.areSameByClass(a2, UnknownArrayAccess.class)) {
		    return UNKNOWNSAFETY;
		} else {
		    // we don't know what to do here
		    return UNKNOWNSAFETY;
		}
	    }
	}

	/**
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are Safe/UnsafeAccess. In this case, rhs is a
         * subtype of lhs iff lhs contains at least every element of rhs
	 * or lhs has no elements (unrestricted safety)
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
	    if (AnnotationUtils.areSameByClass(lhs, UnknownArrayAccess.class)
		|| AnnotationUtils.areSameByClass(rhs, ArrayAccessBottom.class)) {
		return true;
	    } else if (AnnotationUtils.areSameByClass(rhs, UnknownArrayAccess.class)
		       || AnnotationUtils.areSameByClass(lhs, ArrayAccessBottom.class)) {
		return false;
	    } else if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
		// same type, so check subtype
		List<String> lhsValues = AnnotationUtils.getElementValueArray(lhs, "value", String.class, true);
		List<String> rhsValues = AnnotationUtils.getElementValueArray(rhs, "value", String.class, true);
		if (lhsValues.isEmpty()) {
		    return true;
		} else return lhsValues.containsAll(rhsValues);
	    } else {
		return false;
	    }
	}
	
    }
    
}
