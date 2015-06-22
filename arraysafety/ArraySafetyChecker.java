package org.checkerframework.checker.arraysafety;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

import org.checkerframework.checker.arraysafety.qual.UnknownArraySafety;
import org.checkerframework.checker.arraysafety.qual.SafeArrayIndex;
import org.checkerframework.checker.arraysafety.qual.UnsafeArrayIndex;
import org.checkerframework.checker.arraysafety.qual.ArraySafetyBottom;

@TypeQualifiers({UnknownArraySafety.class,SafeArrayIndex.class,UnsafeArrayIndex.class,ArraySafetyBottom.class})
    public class ArraySafetyChecker extends BaseTypeChecker { }
