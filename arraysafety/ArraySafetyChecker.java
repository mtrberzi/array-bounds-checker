package org.checkerframework.checker.arraysafety;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.common.value.ValueChecker;

import java.util.LinkedHashSet;

import org.checkerframework.checker.arraysafety.qual.*;

@TypeQualifiers({
	/*	UnknownArraySafety.class,SafeArrayIndex.class,UnsafeArrayIndex.class,ArraySafetyBottom.class,*/
	    UnknownArrayAccess.class,SafeArrayAccess.class,UnsafeArrayAccess.class,ArrayAccessBottom.class})
public class ArraySafetyChecker extends BaseTypeChecker {
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
	LinkedHashSet<Class<? extends BaseTypeChecker>> subcheckers = new LinkedHashSet<>();
	subcheckers.addAll(super.getImmediateSubcheckerClasses());
	subcheckers.add(ValueChecker.class);
	return subcheckers;
    }
}
