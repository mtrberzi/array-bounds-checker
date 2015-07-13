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

public class ArrayLengthTransfer extends CFAbstractTransfer<CFValue, CFStore, ArrayLengthTransfer> {

    protected AnnotatedTypeFactory atypefactory;
    
    protected ArrayLengthAnalysis analysis;

    public ArrayLengthTransfer(ArrayLengthAnalysis analysis) {
	super(analysis);
	this.analysis = analysis;
	atypefactory = analysis.getTypeFactory();
    }
}
