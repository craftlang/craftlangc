package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.ast.*;

public interface Expression extends Node {
	<T, E extends Throwable> T accept(ExpressionVisitor<T, E> visitor) throws E;
}