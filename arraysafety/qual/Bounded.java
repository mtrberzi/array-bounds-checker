package org.checkerframework.checker.arraysafety.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

@TypeQualifier
//@SubtypeOf(Unbounded.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Bounded {
    Integer lowerBound = Integer.MIN_VALUE;
    Integer upperBound = Integer.MAX_VALUE;
}