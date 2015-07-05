package org.checkerframework.checker.arraysafety;

import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.ArrayType;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.node.*;

public class ArraySafetyTransfer extends CFAbstractTransfer<CFValue, CFStore, ArraySafetyTransfer> {

    protected AnnotatedTypeFactory atypefactory;
    
    protected ArraySafetyAnalysis analysis;

    public ArraySafetyTransfer(ArraySafetyAnalysis analysis) {
	super(analysis);
	this.analysis = analysis;
	atypefactory = analysis.getTypeFactory();
    }

    private AnnotationMirror createUnsafeArrayAccessAnnotation(Set<String> values) {
	return ((ArraySafetyAnnotatedTypeFactory)atypefactory).createUnsafeArrayAccessAnnotation(values);
    }

    private AnnotationMirror createSafeArrayAccessAnnotation(Set<String> values) {
	return ((ArraySafetyAnnotatedTypeFactory)atypefactory).createSafeArrayAccessAnnotation(values);
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
     * (EXPR >= 0) or (0 <= EXPR)
     */
    private boolean nodeIsGEZero(Node node) {
	if (node instanceof GreaterThanOrEqualNode) {
	    GreaterThanOrEqualNode ge = (GreaterThanOrEqualNode)node;
	    if (ge.getRightOperand() instanceof IntegerLiteralNode) {
		IntegerLiteralNode i = (IntegerLiteralNode)ge.getRightOperand();
		if (i.getValue() == 0) {
		    return true;
		} else {
		    return false;
		}
	    } else {
		return false;
	    }
	} else if (node instanceof LessThanOrEqualNode) {
	    LessThanOrEqualNode le = (LessThanOrEqualNode)node;
	    if (le.getLeftOperand() instanceof IntegerLiteralNode) {
		IntegerLiteralNode i = (IntegerLiteralNode)le.getLeftOperand();
		if (i.getValue() == 0) {
		    return true;
		} else {
		    return false;
		}
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
    }

    /* 
     * requires: nodeIsGEZero(node) == true
     * returns: EXPR in (EXPR >= 0) or (0 <= EXPR)
     */
    private Node extractGEZeroExpr(Node node) {
	if (node instanceof GreaterThanOrEqualNode) {
	    GreaterThanOrEqualNode ge = (GreaterThanOrEqualNode)node;
	    return ge.getLeftOperand();
	} else if (node instanceof LessThanOrEqualNode) {
	    LessThanOrEqualNode le = (LessThanOrEqualNode)node;
	    return le.getRightOperand();
	} else {
	    throw new IllegalArgumentException("internal error: precondition failed");
	}
    }
    
    /*
     * returns: true iff the given node is an expression of the form
     * (EXPR < EXPR.length) or (EXPR.length > EXPR)
     */
    private boolean nodeIsLTArrayLength(Node node) {
	if (node instanceof LessThanNode) {
	    LessThanNode lt = (LessThanNode)node;
	    if (lt.getRightOperand() instanceof FieldAccessNode && nodeIsArrayLengthAccess(lt.getRightOperand())) {
		return true;
	    } else {
		return false;
	    }
	} else if (node instanceof GreaterThanNode) {
	    GreaterThanNode gt = (GreaterThanNode)node;
	    if (gt.getLeftOperand() instanceof FieldAccessNode && nodeIsArrayLengthAccess(gt.getLeftOperand())) {
		return true;
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
    }

    /*
     * requires: nodeIsLTArrayLength(node) == true
     * returns: EXPR in (EXPR < ARRAY.length) or (ARRAY.length > EXPR)
     */
    private Node extractLTArrayLengthExpr(Node node) {
	if (node instanceof LessThanNode) {
	    LessThanNode lt = (LessThanNode)node;
	    return lt.getLeftOperand();
	} else if (node instanceof GreaterThanNode) {
	    GreaterThanNode gt = (GreaterThanNode)node;
	    return gt.getRightOperand();
	} else {
	    throw new IllegalArgumentException("internal error: precondition failed");
	}
    }

    /*
     * requires: nodeIsLTArrayLength(node) == true
     * returns: ARRAY in (EXPR < ARRAY.length) or (ARRAY.length > EXPR)
     */
    private Node extractLTArrayLengthArray(Node node) {
	if (node instanceof LessThanNode) {
	    LessThanNode lt = (LessThanNode)node;
	    FieldAccessNode fieldAccess = (FieldAccessNode)lt.getRightOperand();
	    return fieldAccess.getReceiver();
	} else if (node instanceof GreaterThanNode) {
	    GreaterThanNode gt = (GreaterThanNode)node;
	    FieldAccessNode fieldAccess = (FieldAccessNode)gt.getLeftOperand();
	    return fieldAccess.getReceiver();
	} else {
	    throw new IllegalArgumentException("internal error: precondition failed");
	}
    }
    
    public TransferResult<CFValue, CFStore>
	visitConditionalAnd(ConditionalAndNode n, TransferInput<CFValue, CFStore> p) {
	TransferResult<CFValue, CFStore> result = super.visitConditionalAnd(n, p);
	// look for expressions of the form
	// I >= 0 && I < A.length
	Node lhs = n.getLeftOperand();
	Node rhs = n.getRightOperand();

	if (nodeIsGEZero(lhs) && nodeIsLTArrayLength(rhs)) {
	    Node expr1 = extractGEZeroExpr(lhs);
	    Node expr2 = extractLTArrayLengthExpr(rhs);
	    if (expr1.equals(expr2)) {
		Node arrayExpr = extractLTArrayLengthArray(rhs);
		throw new UnsupportedOperationException("bogus 1");
	    }
	} else if (nodeIsLTArrayLength(lhs) && nodeIsGEZero(rhs)) {
	    throw new UnsupportedOperationException("bogus 2");
	}
	
	return result;
    }
    
}
