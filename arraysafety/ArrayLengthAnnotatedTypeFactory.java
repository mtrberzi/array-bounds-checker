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

public class ArrayLengthAnnotatedTypeFactory extends GenericAnnotatedTypeFactory<CFValue, CFStore, ArrayLengthTransfer, ArrayLengthAnalysis> {

    protected final AnnotationMirror UNKNOWN;
    protected final AnnotationMirror BOTTOM;
    
    public ArrayLengthAnnotatedTypeFactory(BaseTypeChecker checker) {
	super(checker);

	UNKNOWN = AnnotationUtils.fromClass(elements, UnknownArrayLength.class);
	BOTTOM = AnnotationUtils.fromClass(elements, ArrayLengthBottom.class);
	
	this.postInit();
    }
    
    @Override
    protected ArrayLengthAnalysis createFlowAnalysis(List<Pair<VariableElement, CFValue>> fieldValues) {
	return new ArrayLengthAnalysis(checker, this, fieldValues);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
	return new ListTreeAnnotator(
				     new ImplicitsTreeAnnotator(this),
				     new ArrayLengthTreeAnnotator(this)
				     );
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
	return new ArrayLengthQualifierHierarchy(factory);
    }

    @Override
    protected QualifierDefaults createQualifierDefaults() {
	QualifierDefaults defaults = super.createQualifierDefaults();
	defaults.addAbsoluteDefault(UNKNOWN, DefaultLocation.OTHERWISE);
	defaults.addAbsoluteDefault(BOTTOM, DefaultLocation.LOWER_BOUNDS);

	return defaults;
    }

    protected AnnotationMirror createLessThanAnnotation(Set<String> valueSet) {
	AnnotationBuilder builder = new AnnotationBuilder(processingEnv, LessThanArrayLength.class);
	List<String> values = new ArrayList<String>(valueSet);
	builder.setValue("values", values);
	return builder.build();
    }
    
    private class ArrayLengthTreeAnnotator extends TreeAnnotator {
	public ArrayLengthTreeAnnotator(AnnotatedTypeFactory aTypeFactory) {
	    super(aTypeFactory);
	}
	// all the interesting stuff happens in dataflow...
    }
	

    private final class ArrayLengthQualifierHierarchy extends MultiGraphQualifierHierarchy {

	public ArrayLengthQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
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
                return BOTTOM;
            }
        }

	/**
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are LessThanArrayLength.
	 * In this case, rhs is a subtype of lhs
	 * iff lhs is a subset of rhs
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
	    if (AnnotationUtils.areSameByClass(lhs, UnknownArrayLength.class)
		|| AnnotationUtils.areSameByClass(rhs, ArrayLengthBottom.class)) {
		return true;
	    } else if (AnnotationUtils.areSameByClass(rhs, UnknownArrayLength.class)
		       || AnnotationUtils.areSameByClass(lhs, ArrayLengthBottom.class)) {
		return false;
	    } else if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
		// both are LessThanArrayLength
		List<String> lhsValues = AnnotationUtils.getElementValueArray(lhs, "values", String.class, true);
		List<String> rhsValues = AnnotationUtils.getElementValueArray(rhs, "values", String.class, true);
		return rhsValues.containsAll(lhsValues);
	    } else {
		return false;
	    }
	}
	
    }
    
}
