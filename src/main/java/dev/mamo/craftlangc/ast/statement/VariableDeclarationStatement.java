package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.ast.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class VariableDeclarationStatement implements Statement, Serializable {
	private ParseNode source;
	private FQN variableTypeFQN;
	private String variableName;

	public VariableDeclarationStatement(ParseNode source, FQN variableTypeFQN, String variableName) {
		setSource(source);
		setVariableTypeFQN(variableTypeFQN);
		setVariableName(variableName);
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
		this.variableTypeFQN = Objects.requireNonNull(variableTypeFQN);
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = Objects.requireNonNull(variableName);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TypeAndName)) {
			return false;
		}
		TypeAndName typeAndName = (TypeAndName) obj;
		return typeAndName.getSource().equals(getSource())
			&& typeAndName.getTypeFQN().equals(getVariableTypeFQN())
			&& typeAndName.getName().equals(getVariableName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getVariableTypeFQN(),
			getVariableName()
		);
	}

	@Override
	public <T, E extends Throwable> T accept(StatementVisitor<T, E> visitor) throws E {
		return visitor.visitVariableDeclarationStatement(this);
	}
}