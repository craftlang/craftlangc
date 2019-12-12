package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.*;

public interface Statement extends Node {
	<T, E extends Throwable> T accept(StatementVisitor<T, E> visitor) throws E;
}