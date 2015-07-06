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
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.checker.arraysafety.qual.*;

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
	    Node idxExpr = extractGEZeroExpr(lhs);
	    Node idxExpr2 = extractLTArrayLengthExpr(rhs);
	    if (idxExpr.equals(idxExpr2)) {		
		Node arrayExpr = extractLTArrayLengthArray(rhs);
		// figure out what array is being referred to
		FlowExpressions.Receiver arrayReceiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), arrayExpr);
		String arrayRef = null;
		if (arrayReceiver instanceof FlowExpressions.LocalVariable) {
		    FlowExpressions.LocalVariable var = (FlowExpressions.LocalVariable)arrayReceiver;
		    Element varEl = var.getElement();
		    arrayRef = varEl.getSimpleName().toString();
		}
		
		if (arrayRef == null) {
		    // couldn't figure out a good name for this array
		    return result;
		}

		CFValue value = p.getValueOfSubNode(idxExpr);
		boolean knownSafe = false;
		boolean knownUnsafe = false;

		AnnotationMirror safetyAnno = value.getType().getAnnotation(SafeArrayAccess.class);
		if (safetyAnno != null) {
		    knownSafe = true;
		}

		safetyAnno = value.getType().getAnnotation(UnsafeArrayAccess.class);
		if (safetyAnno != null) {
		    knownUnsafe = true;
		}
		
		// TODO preserve existing safety/unsafety information
		Set<String> safeArrays = new HashSet<String>();
		safeArrays.add(arrayRef);
		Set<String> unsafeArrays = new HashSet<String>();
		unsafeArrays.add(arrayRef);
		
		CFStore thenStore = result.getThenStore();
		CFStore elseStore = result.getElseStore();
		FlowExpressions.Receiver indexReceiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), idxExpr);
		
		// in the 'then' store, idxExpr is safe wrt. arrayExpr
		thenStore.insertValue(indexReceiver, createSafeArrayAccessAnnotation(safeArrays));
		// in the 'else' store, idxExpr is unsafe wrt. arrayExpr
		elseStore.insertValue(indexReceiver, createUnsafeArrayAccessAnnotation(unsafeArrays));

		return new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
	    }
	} else if (nodeIsLTArrayLength(lhs) && nodeIsGEZero(rhs)) {
	    throw new UnsupportedOperationException("bogus 2");
	}
	
	return result;
    }
    
}
