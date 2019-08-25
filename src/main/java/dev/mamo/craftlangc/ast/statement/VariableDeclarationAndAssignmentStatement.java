package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class VariableDeclarationAndAssignmentStatement implements Statement, Serializable {
	private final ParseNode source;
	private final Type type;
	private final String name;
	private final Expression value;

	public VariableDeclarationAndAssignmentStatement(ParseNode source, Type type, String name, Expression value) {
		this.source = Objects.requireNonNull(source);
		this.type = type;
		this.name = Objects.requireNonNull(name);
		this.value = Objects.requireNonNull(value);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Expression getValue() {
		return value;
	}

	@Override
	public <T, U extends Throwable> T accept(StatementVisitor<T, U> visitor) throws U {
		return visitor.visitVariableDeclarationAndAssignmentStatement(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VariableDeclarationAndAssignmentStatement)) {
			return false;
		}
		VariableDeclarationAndAssignmentStatement statement = (VariableDeclarationAndAssignmentStatement) obj;
		return statement.getSource().equals(getSource())
			&& statement.getType().equals(getType())
			&& statement.getName().equals(getName())
			&& statement.getValue().equals(getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getType(),
			getName(),
			getValue()
		);
	}
}