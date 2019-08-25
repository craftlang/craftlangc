package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class UnaryExpression implements Expression, Serializable {
	private final ParseNode source;
	private final UnaryOperator operator;
	private final Expression operand;

	public UnaryExpression(ParseNode source, UnaryOperator operator, Expression operand) {
		this.source = Objects.requireNonNull(source);
		this.operator = Objects.requireNonNull(operator);
		this.operand = Objects.requireNonNull(operand);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public UnaryOperator getOperator() {
		return operator;
	}

	public Expression getOperand() {
		return operand;
	}

	@Override
	public <T, U extends Throwable> T accept(ExpressionVisitor<T, U> visitor) throws U {
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