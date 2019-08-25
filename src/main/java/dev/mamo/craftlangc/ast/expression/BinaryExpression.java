package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class BinaryExpression implements Expression, Serializable {
	private final ParseNode source;
	private final Expression left;
	private final BinaryOperator operator;
	private final Expression right;

	public BinaryExpression(ParseNode source, Expression left, BinaryOperator operator, Expression right) {
		this.source = Objects.requireNonNull(source);
		this.left = Objects.requireNonNull(left);
		this.operator = Objects.requireNonNull(operator);
		this.right = Objects.requireNonNull(right);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public Expression getLeft() {
		return left;
	}

	public BinaryOperator getOperator() {
		return operator;
	}

	public Expression getRight() {
		return right;
	}

	@Override
	public <T, U extends Throwable> T accept(ExpressionVisitor<T, U> visitor) throws U {
		return visitor.visitBinaryExpression(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof BinaryExpression)) {
			return false;
		}
		BinaryExpression expression = (BinaryExpression) obj;
		return expression.getSource().equals(getSource())
			&& expression.getLeft().equals(getLeft())
			&& expression.getOperator().equals(getOperator())
			&& expression.getRight().equals(getRight());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getLeft(),
			getOperator(),
			getRight()
		);
	}

}