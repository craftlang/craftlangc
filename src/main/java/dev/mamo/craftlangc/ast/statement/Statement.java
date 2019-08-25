package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.*;

public interface Statement extends Node {
	<T, U extends Throwable> T accept(StatementVisitor<T, U> visitor) throws U;
}