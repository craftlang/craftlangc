package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.ast.*;

public interface Expression extends Node {
	<T, U extends Throwable> T accept(ExpressionVisitor<T, U> visitor) throws U;
}