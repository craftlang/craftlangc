package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class BinaryExpression implements Expression, Serializable {
	private ParseNode source;
	private Expression left;
	private BinaryOperator operator;
	private Expression right;

	public BinaryExpression(ParseNode source, Expression left, BinaryOperator operator, Expression right) {
		setSource(source);
		setLeft(left);
		setOperator(operator);
		setRight(right);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public Expression getLeft() {
		return left;
	}

	public void setLeft(Expression left) {
		this.left = Objects.requireNonNull(left);
	}

	public BinaryOperator getOperator() {
		return operator;
	}

	public void setOperator(BinaryOperator operator) {
		this.operator = Objects.requireNonNull(operator);
	}

	public Expression getRight() {
		return right;
	}

	public void setRight(Expression right) {
		this.right = Objects.requireNonNull(right);
	}

	@Override
	public <T, E extends Throwable> T accept(ExpressionVisitor<T, E> visitor) throws E {
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