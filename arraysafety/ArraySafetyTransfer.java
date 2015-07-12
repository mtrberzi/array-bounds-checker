package org.checkerframework.checker.arraysafety;

import java.util.Set;
import java.util.HashSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.ArrayType;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.checker.arraysafety.qual.*;
import org.checkerframework.javacutil.AnnotationUtils;

public class ArraySafetyTransfer extends CFAbstractTransfer<CFValue, CFStore, ArraySafetyTransfer> {

    protected AnnotatedTypeFactory atypefactory;
    
    protected ArraySafetyAnalysis analysis;

    public ArraySafetyTransfer(ArraySafetyAnalysis analysis) {
	super(analysis);
	this.analysis = analysis;
	atypefactory = analysis.getTypeFactory();
    }

    private AnnotationMirror createBoundedAnnotation(Integer lowerBound, Integer upperBound) {
	return ((ArraySafetyAnnotatedTypeFactory)atypefactory).createBoundedAnnotation(lowerBound, upperBound);
    }

    private AnnotationMirror createUnboundedAnnotation() {
	return ((ArraySafetyAnnotatedTypeFactory)atypefactory).UNBOUNDED;
    }

    // returns: unbounded result value
    private TransferResult<CFValue, CFStore> createNewResult(TransferResult<CFValue, CFStore> result) {
	AnnotationMirror bounds = createUnboundedAnnotation();
	CFValue newResultValue = analysis.createSingleAnnotationValue(bounds, result.getResultValue().getType().getUnderlyingType());
	return new RegularTransferResult<>(newResultValue, result.getRegularStore());
    }

    // returns: bounded result value
    private TransferResult<CFValue, CFStore> createNewResult(TransferResult<CFValue, CFStore> result, Integer lowerBound, Integer upperBound) {
	AnnotationMirror bounds = createBoundedAnnotation(lowerBound, upperBound);
	CFValue newResultValue = analysis.createSingleAnnotationValue(bounds, result.getResultValue().getType().getUnderlyingType());
	return new RegularTransferResult<>(newResultValue, result.getRegularStore());
    }

    private Integer getLowerBound(Node subNode, TransferInput<CFValue, CFStore> p) {
	CFValue value = p.getValueOfSubNode(subNode);
	AnnotationMirror boundedAnno = value.getType().getAnnotation(Bounded.class);
	if (boundedAnno == null) {
	    throw new IllegalArgumentException("node is not Bounded");
	} else {
	    return AnnotationUtils.getElementValue(boundedAnno, "lowerBound", Integer.class, true);
	}
    }

    private Integer getUpperBound(Node subNode, TransferInput<CFValue, CFStore> p) {
	CFValue value = p.getValueOfSubNode(subNode);
	AnnotationMirror boundedAnno = value.getType().getAnnotation(Bounded.class);
	if (boundedAnno == null) {
	    throw new IllegalArgumentException("node is not Bounded");
	} else {
	    return AnnotationUtils.getElementValue(boundedAnno, "upperBound", Integer.class, true);
	}
    }

    private boolean isUnbounded(Node subNode, TransferInput<CFValue, CFStore> p) {
	CFValue value = p.getValueOfSubNode(subNode);
	AnnotationMirror boundedAnno = value.getType().getAnnotation(Unbounded.class);
	return (boundedAnno != null);
    }

    // returns: true iff value < Integer.MIN_VALUE or value > Integer.MAX_VALUE
    private boolean notAnInteger(Long value) {
	Long min = Long.valueOf(Integer.MIN_VALUE);
	Long max = Long.valueOf(Integer.MAX_VALUE);
	return (value < min || value > max);
    }
    
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(NumericalSubtractionNode n, TransferInput<CFValue, CFStore> p) {
	TransferResult<CFValue, CFStore> transferResult = super.visitNumericalSubtraction(n, p);
	Node lhs = n.getLeftOperand();
	Node rhs = n.getRightOperand();
	if (isUnbounded(lhs, p) || isUnbounded(rhs, p)) {
	    return createNewResult(transferResult);
	} else {
	    Long Llhs = getLowerBound(lhs, p).longValue();
	    Long Lrhs = getLowerBound(rhs, p).longValue();
	    Long Ulhs = getUpperBound(lhs, p).longValue();
	    Long Urhs = getUpperBound(rhs, p).longValue();

	    Long newLowerBound = Llhs - Urhs;
	    Long newUpperBound = Ulhs - Lrhs;
	    if (notAnInteger(newLowerBound) || notAnInteger(newUpperBound)) {
		return createNewResult(transferResult);
	    } else {
		return createNewResult(transferResult, newLowerBound.intValue(), newUpperBound.intValue());
	    }
	}
    }
}
