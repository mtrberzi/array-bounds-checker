package org.checkerframework.checker.arraysafety.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the ArraySafety qualifier hierarchy.
 * This is used to make the null literal a subtype of all ArraySafety annotations.
 */

@TypeQualifier
@InvisibleQualifier
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
	     typeNames = {java.lang.Void.class})
	     @SubtypeOf({SafeArrayAccess.class,UnsafeArrayAccess.class})
	     @DefaultFor(value={DefaultLocation.LOWER_BOUNDS})
	     @Target({}) // empty target prevents programmers from writing this in a program
	     public @interface ArrayAccessBottom {}
