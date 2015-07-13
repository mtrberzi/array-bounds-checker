package org.checkerframework.checker.arraysafety;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
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

public class ArrayLengthTransfer extends CFAbstractTransfer<CFValue, CFStore, ArrayLengthTransfer> {

    protected AnnotatedTypeFactory atypefactory;
    
    protected ArrayLengthAnalysis analysis;

    public ArrayLengthTransfer(ArrayLengthAnalysis analysis) {
	super(analysis);
	this.analysis = analysis;
	atypefactory = analysis.getTypeFactory();
    }

    protected AnnotationMirror createLessThanAnnotation(Set<String> valueSet) {
	return ((ArrayLengthAnnotatedTypeFactory)atypefactory).createLessThanAnnotation(valueSet);
    }
    
    // returns: true iff the given node has annotation LessThanArrayLength
    private boolean hasLTArrayLengthAnnotation(Node subNode, TransferInput<CFValue, CFStore> p) {
	CFValue value = p.getValueOfSubNode(subNode);
	AnnotationMirror ltAnno = value.getType().getAnnotation(LessThanArrayLength.class);
	return (ltAnno != null);
    }

    private CFValue toCFValue(AnnotationMirror a, Receiver r) {
	return analysis.createSingleAnnotationValue(a, r.getType());
    }
    
    // precondition: hasLTArrayLengthAnnotation(subNode, p) == true
    // returns: arrays named in that annotation on subNode
    private List<String> getLTArrays(Node subNode, TransferInput<CFValue, CFStore> p) {
	CFValue value = p.getValueOfSubNode(subNode);
	AnnotationMirror ltAnno = value.getType().getAnnotation(LessThanArrayLength.class);
	if (ltAnno == null) {
	    return new ArrayList<String>();
	} else {
	    return AnnotationUtils.getElementValueArray(ltAnno, "values", String.class, true);
	}
    }
    
    /*
     * returns: true iff the given node is an expression of the form
     * (EXPR.length) and EXPR has array type
     */
    private boolean nodeIsArrayLengthAccess(Node node) {
	if (!(node instanceof FieldAccessNode)) {
	    return false;
	}
	FieldAccessNode fieldAccess = (FieldAccessNode)node;
	String field = fieldAccess.getFieldName();
	if (!(field.equals("length"))) {
	    return false;
	}

	// type-check expression
	TypeMirror exprType = fieldAccess.getReceiver().getType();
	if (!(exprType instanceof ArrayType)) {
	    return false;
	}

	return true;
    }
    
    /*
     * returns: true iff the given node is an expression of the form
     * ( EXPR < [ARRAY].length ) or ( [ARRAY].length > EXPR )
     */
    private boolean nodeIsLessThanArrayLength(Node node) {
	if (node instanceof LessThanNode) {
	    LessThanNode lt = (LessThanNode)node;
	    if (nodeIsArrayLengthAccess(lt.getRightOperand())) {
		return true;
	    } else {
		return false;
	    }
	} else if (node instanceof GreaterThanNode) {
	    GreaterThanNode gt = (GreaterThanNode)node;
	    if (nodeIsArrayLengthAccess(gt.getLeftOperand())) {
		return true;
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(LessThanNode n, TransferInput<CFValue, CFStore> p) {
	TransferResult<CFValue, CFStore> transferResult = super.visitLessThan(n, p);
	if (!(nodeIsLessThanArrayLength(n))) {
	    return transferResult;
	}
	// LHS gains LessThanArrayLength(rhs!array)
	Node lhs = n.getLeftOperand();
	Receiver lhsReceiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), lhs);
	Node array = ((FieldAccessNode)n.getRightOperand()).getReceiver();
	Receiver arrayReceiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), array);
	String arrayName = arrayReceiver.toString(); // good enough for the Map Key Checker...
	
	Set<String> annoValues = new HashSet<String>();
	annoValues.add(arrayName);
	if (hasLTArrayLengthAnnotation(lhs, p)) {
	    annoValues.addAll(getLTArrays(lhs, p));
	}
	CFValue ltAnno = toCFValue(createLessThanAnnotation(annoValues), lhsReceiver);
	CFStore thenStore = transferResult.getRegularStore();
	CFStore elseStore = thenStore.copy();
	thenStore.replaceValue(lhsReceiver, ltAnno);
	
	return new ConditionalTransferResult<>(transferResult.getResultValue(), thenStore, elseStore);
    }
    
}
