package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class VariableAssignmentStatement implements Statement, Serializable {
	private ParseNode source;
	private FQN variableFQN;
	private AssignmentOperator operator;
	private Expression value;

	public VariableAssignmentStatement(ParseNode source, FQN variableFQN, AssignmentOperator operator, Expression value) {
		setSource(source);
		setVariableFQN(variableFQN);
		setOperator(operator);
		setValue(value);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public FQN getVariableFQN() {
		return variableFQN;
	}

	public void setVariableFQN(FQN variableFQN) {
		this.variableFQN = Objects.requireNonNull(variableFQN);
	}

	public AssignmentOperator getOperator() {
		return operator;
	}

	public void setOperator(AssignmentOperator operator) {
		this.operator = Objects.requireNonNull(operator);
	}

	public Expression getValue() {
		return value;
	}

	public void setValue(Expression value) {
		this.value = Objects.requireNonNull(value);
	}

	@Override
	public <T, E extends Throwable> T accept(StatementVisitor<T, E> visitor) throws E {
		return visitor.visitVariableAssignmentStatement(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VariableAssignmentStatement)) {
			return false;
		}
		VariableAssignmentStatement statement = (VariableAssignmentStatement) obj;
		return statement.getSource().equals(getSource())
			&& statement.getVariableFQN().equals(getVariableFQN())
			&& statement.getOperator().equals(getOperator())
			&& statement.getValue().equals(getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSource(),
			getVariableFQN(),
			getOperator(),
			getValue()
		);
	}
}