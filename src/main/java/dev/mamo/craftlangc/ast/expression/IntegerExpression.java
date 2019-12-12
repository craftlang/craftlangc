package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class IntegerExpression implements Expression, Serializable {
	private ParseNode source;
	private int value;

	public IntegerExpression(ParseNode source, int value) {
		setSource(source);
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

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public <T, E extends Throwable> T accept(ExpressionVisitor<T, E> visitor) throws E {
		return visitor.visitIntegerExpression(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IntegerExpression)) {
			return false;
		}
		IntegerExpression expression = (IntegerExpression) obj;
		return expression.getSource().equals(getSource())
			&& expression.getValue() == getValue();
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getValue()
		);
	}
}