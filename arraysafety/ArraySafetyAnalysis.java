package org.checkerframework.checker.arraysafety;

import java.util.List;

import javax.lang.model.element.VariableElement;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.Pair;

public class ArraySafetyAnalysis extends CFAbstractAnalysis<CFValue, CFStore, ArraySafetyTransfer> {

    public ArraySafetyAnalysis(BaseTypeChecker checker,
			       ArraySafetyAnnotatedTypeFactory factory,
			       List<Pair<VariableElement, CFValue>> fieldValues) {
	super(checker, factory, fieldValues);
    }

    @Override
    public ArraySafetyTransfer createTransferFunction() {
	return new ArraySafetyTransfer(this);
    }

    @Override
    public CFStore createEmptyStore(boolean sequentialSemantics) {
	return new CFStore(this, sequentialSemantics);
    }

    @Override
    public CFStore createCopiedStore(CFStore s) {
	return new CFStore(this, s);
    }

    @Override
    public CFValue createAbstractValue(AnnotatedTypeMirror type) {
	return defaultCreateAbstractValue(this, type);
    }
    
}
