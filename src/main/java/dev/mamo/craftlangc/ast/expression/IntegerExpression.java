package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class IntegerExpression implements Expression, Serializable {
	private final ParseNode source;
	private final int integer;

	public IntegerExpression(ParseNode source, int integer) {
		this.source = source;
		this.integer = integer;
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public int getInteger() {
		return integer;
	}

	@Override
	public <T, U extends Throwable> T accept(ExpressionVisitor<T, U> visitor) throws U {
		return visitor.visitIntegerExpression(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IntegerExpression)) {
			return false;
		}
		IntegerExpression expression = (IntegerExpression) obj;
		return expression.getSource().equals(getSource())
			&& expression.getInteger() == getInteger();
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getInteger()
		);
	}
}