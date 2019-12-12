package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class VariableDeclarationAndAssignmentStatement implements Statement, Serializable {
	private ParseNode source;
	private FQN variableTypeFQN;
	private String variableName;
	private Expression assignedValue;

	public VariableDeclarationAndAssignmentStatement(ParseNode source, FQN variableTypeFQN, String variableName, Expression assignedValue) {
		setSource(source);
		setVariableTypeFQN(variableTypeFQN);
		setVariableName(variableName);
		setAssignedValue(assignedValue);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public FQN getVariableTypeFQN() {
		return variableTypeFQN;
	}

	public void setVariableTypeFQN(FQN variableTypeFQN) {
		this.variableTypeFQN = variableTypeFQN;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = Objects.requireNonNull(variableName);
	}

	public Expression getAssignedValue() {
		return assignedValue;
	}

	public void setAssignedValue(Expression assignedValue) {
		this.assignedValue = Objects.requireNonNull(assignedValue);
	}

	@Override
	public <T, E extends Throwable> T accept(StatementVisitor<T, E> visitor) throws E {
		return visitor.visitVariableDeclarationAndAssignmentStatement(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VariableDeclarationAndAssignmentStatement)) {
			return false;
		}
		VariableDeclarationAndAssignmentStatement statement = (VariableDeclarationAndAssignmentStatement) obj;
		return statement.getSource().equals(getSource())
			&& statement.getVariableTypeFQN().equals(getVariableTypeFQN())
			&& statement.getVariableName().equals(getVariableName())
			&& statement.getAssignedValue().equals(getAssignedValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getVariableTypeFQN(),
			getVariableName(),
			getAssignedValue()
		);
	}
}