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
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
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

    private CFValue toCFValue(AnnotationMirror a, Receiver r) {
	return analysis.createSingleAnnotationValue(a, r.getType());
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

    // TODO refactor this into a helper function, I don't want to write this code four times
    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
	TransferResult<CFValue, CFStore> transferResult = super.visitGreaterThanOrEqual(n, p);
	Node lhs = n.getLeftOperand();
	Node rhs = n.getRightOperand();
	if (isUnbounded(lhs, p) && isUnbounded(rhs, p)) {
	    return createNewResult(transferResult);
	} else {
	    CFStore thenStore = transferResult.getRegularStore();
	    CFStore elseStore = thenStore.copy();
	    if (!isUnbounded(lhs, p)) {
		// we can learn something about the RHS
		Integer lhsLowerBound = getLowerBound(lhs, p);
		Integer lhsUpperBound = getUpperBound(lhs, p);
		Receiver rhsReceiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), rhs);
		if (isUnbounded(rhs, p)) {
		    // infer new bounds:
		    // if the comparison is true, the lower bound for the RHS
		    // is the lower bound of the LHS
		    CFValue trueBound = toCFValue(createBoundedAnnotation(lhsLowerBound, Integer.MAX_VALUE), rhsReceiver);
		    thenStore.replaceValue(rhsReceiver, trueBound);
		    // if the comparison is false, the lower bound for the RHS
		    // is one greater than the lower bound of the LHS
		    if (lhsLowerBound != Integer.MAX_VALUE) {
			CFValue falseBound = toCFValue(createBoundedAnnotation(Integer.MIN_VALUE, lhsLowerBound + 1), rhsReceiver);
			elseStore.replaceValue(rhsReceiver, falseBound);
		    }
		} else {
		    // update existing bounds
		    Integer rhsLowerBound = getLowerBound(rhs, p);
		    Integer rhsUpperBound = getUpperBound(rhs, p);
		    // there are three cases.
		    // case 1: the LHS interval is strictly below the RHS interval.
		    // case 2: the LHS interval is strictly above the RHS interval (or the upper and lower bounds are touching).
		    // case 3: the LHS interval overlaps the RHS interval.
		    if (lhsUpperBound < rhsLowerBound) {
			// case 1
			// 'then' branch is never taken
			// TODO
		    } else if (lhsLowerBound >= rhsUpperBound) {
			// case 2
			// 'else' branch is never taken
			// TODO
		    } else {
			// case 3
			// both branches can be taken
			// in the true branch, the RHS lower bound stays the same
			// and the RHS upper bound is equal to the LHS upper bound
			CFValue trueBound = toCFValue(createBoundedAnnotation(rhsLowerBound, lhsUpperBound), rhsReceiver);
			thenStore.replaceValue(rhsReceiver, trueBound);
			// in the false branch, the RHS upper bound stays the same
			// and the RHS lower bound is one greater than the LHS lower bound
			CFValue falseBound = toCFValue(createBoundedAnnotation(lhsLowerBound+1, rhsUpperBound), rhsReceiver);
			elseStore.replaceValue(rhsReceiver, falseBound);
		    }
		}
	    }
	    if (!isUnbounded(rhs, p)) {
		// we can learn something about the LHS
		Integer rhsLowerBound = getLowerBound(rhs, p);
		Integer rhsUpperBound = getUpperBound(rhs, p);
		Receiver lhsReceiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), lhs);
		if (isUnbounded(lhs, p)) {
		    // infer new bounds:
		    // if the comparison is true, the lower bound for the LHS
		    /// is the lower bound for the RHS
		    CFValue trueBound = toCFValue(createBoundedAnnotation(rhsLowerBound, Integer.MAX_VALUE), lhsReceiver);
		    thenStore.replaceValue(lhsReceiver, trueBound);
		    // if the comparison is false, the upper bound for the LHS
		    // is one less than the lower bound of the RHS
		    if (rhsLowerBound != Integer.MIN_VALUE) {
			CFValue falseBound = toCFValue(createBoundedAnnotation(Integer.MIN_VALUE, rhsLowerBound - 1), lhsReceiver);
			elseStore.replaceValue(lhsReceiver, falseBound);
		    }
		} else {
		    // update existing bounds
		    // TODO
		}
	    }
	    return new ConditionalTransferResult<>(transferResult.getResultValue(), thenStore, elseStore);
	}
    }
    
    @Override
    public TransferResult<CFValue, CFStore> visitNumericalAddition(NumericalAdditionNode n, TransferInput<CFValue, CFStore> p) {
	TransferResult<CFValue, CFStore> transferResult = super.visitNumericalAddition(n, p);
	Node lhs = n.getLeftOperand();
	Node rhs = n.getRightOperand();
	if (isUnbounded(lhs, p) || isUnbounded(rhs, p)) {
	    return createNewResult(transferResult);
	} else {
	    Long Llhs = getLowerBound(lhs, p).longValue();
	    Long Lrhs = getLowerBound(rhs, p).longValue();
	    Long Ulhs = getUpperBound(lhs, p).longValue();
	    Long Urhs = getUpperBound(rhs, p).longValue();

	    Long newLowerBound = Llhs + Lrhs;
	    Long newUpperBound = Ulhs + Urhs;
	    if (notAnInteger(newLowerBound) || notAnInteger(newUpperBound)) {
		return createNewResult(transferResult);
	    } else {
		return createNewResult(transferResult, newLowerBound.intValue(), newUpperBound.intValue());
	    }
	}
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalMinus(NumericalMinusNode n, TransferInput<CFValue, CFStore> p) {
	TransferResult<CFValue, CFStore> transferResult = super.visitNumericalMinus(n, p);
	Node expr = n.getOperand();
	if (isUnbounded(expr, p)) {
	    return createNewResult(transferResult);
	} else {
	    Long L = getLowerBound(expr, p).longValue();
	    Long U = getUpperBound(expr, p).longValue();
	    Long newLowerBound = -U;
	    Long newUpperBound = -L;
	    if (notAnInteger(newLowerBound) || notAnInteger(newUpperBound)) {
		return createNewResult(transferResult);
	    } else {
		return createNewResult(transferResult, newLowerBound.intValue(), newUpperBound.intValue());
	    }
	}
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
