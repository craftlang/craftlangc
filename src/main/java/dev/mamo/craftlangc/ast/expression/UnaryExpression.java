package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class UnaryExpression implements Expression, Serializable {
	private ParseNode source;
	private UnaryOperator operator;
	private Expression operand;

	public UnaryExpression(ParseNode source, UnaryOperator operator, Expression operand) {
		setSource(source);
		setOperator(operator);
		setOperand(operand);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public UnaryOperator getOperator() {
		return operator;
	}

	public void setOperator(UnaryOperator operator) {
		this.operator = Objects.requireNonNull(operator);
	}

	public Expression getOperand() {
		return operand;
	}

	public void setOperand(Expression operand) {
		this.operand = Objects.requireNonNull(operand);
	}

	@Override
	public <T, E extends Throwable> T accept(ExpressionVisitor<T, E> visitor) throws E {
		return visitor.visitUnaryExpression(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof UnaryExpression)) {
			return false;
		}
		UnaryExpression expression = (UnaryExpression) obj;
		return expression.getSource().equals(getSource())
			&& expression.getOperator().equals(getOperator())
			&& expression.getOperand().equals(getOperand());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getOperator(),
			getOperand()
		);
	}

}