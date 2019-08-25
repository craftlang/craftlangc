package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class VariableExpression implements Expression, Serializable {
	private final ParseNode source;
	private final QualifiedName name;

	public VariableExpression(ParseNode source, QualifiedName name) {
		this.source = Objects.requireNonNull(source);
		this.name = Objects.requireNonNull(name);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public QualifiedName getName() {
		return name;
	}

	@Override
	public <T, U extends Throwable> T accept(ExpressionVisitor<T, U> visitor) throws U {
		return visitor.visitVariableExpression(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VariableExpression)) {
			return false;
		}
		VariableExpression expression = (VariableExpression) obj;
		return expression.getSource().equals(getSource())
			&& expression.getName().equals(getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getName()
		);
	}
}