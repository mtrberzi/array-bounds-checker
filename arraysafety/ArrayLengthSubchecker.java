package org.checkerframework.checker.arraysafety;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.common.value.ValueChecker;

import java.util.LinkedHashSet;

import org.checkerframework.checker.arraysafety.qual.*;

@TypeQualifiers({
	UnknownArrayLength.class, LessThanArrayLength.class, ArrayLengthBottom.class
	    })
public class ArrayLengthSubchecker extends BaseTypeChecker {
}
