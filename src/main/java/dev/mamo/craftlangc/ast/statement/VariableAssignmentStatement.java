package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class VariableAssignmentStatement implements Statement, Serializable {
	private final ParseNode source;
	private final QualifiedName name;
	private final AssignmentOperator operator;
	private final Expression value;

	public VariableAssignmentStatement(ParseNode source, QualifiedName name, AssignmentOperator operator, Expression value) {
		this.source = Objects.requireNonNull(source);
		this.name = Objects.requireNonNull(name);
		this.operator = Objects.requireNonNull(operator);
		this.value = Objects.requireNonNull(value);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public QualifiedName getName() {
		return name;
	}

	public AssignmentOperator getOperator() {
		return operator;
	}

	public Expression getValue() {
		return value;
	}

	@Override
	public <T, U extends Throwable> T accept(StatementVisitor<T, U> visitor) throws U {
		return visitor.visitVariableAssignmentStatement(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VariableAssignmentStatement)) {
			return false;
		}
		VariableAssignmentStatement statement = (VariableAssignmentStatement) obj;
		return statement.getSource().equals(getSource())
			&& statement.getName().equals(getName())
			&& statement.getOperator().equals(getOperator())
			&& statement.getValue().equals(getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSource(),
			getName(),
			getOperator(),
			getValue()
		);
	}
}