package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class VariableExpression implements Expression, Serializable {
	private ParseNode source;
	private FQN variableFQN;

	public VariableExpression(ParseNode source, FQN variableFQN) {
		setSource(source);
		setFQN(variableFQN);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public FQN getFQN() {
		return variableFQN;
	}

	public void setFQN(FQN fqn) {
		this.variableFQN = Objects.requireNonNull(fqn);
	}

	@Override
	public <T, E extends Throwable> T accept(ExpressionVisitor<T, E> visitor) throws E {
		return visitor.visitVariableExpression(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VariableExpression)) {
			return false;
		}
		VariableExpression expression = (VariableExpression) obj;
		return expression.getSource().equals(getSource())
			&& expression.getFQN().equals(getFQN());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getFQN()
		);
	}
}