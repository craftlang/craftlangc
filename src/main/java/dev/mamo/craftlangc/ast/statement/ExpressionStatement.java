package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class ExpressionStatement implements Statement, Serializable {
	private ParseNode source;
	private Expression expression;

	public ExpressionStatement(ParseNode source, Expression expression) {
		setSource(source);
		setExpression(expression);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public Expression getExpression() {
		return expression;
	}

	public void setExpression(Expression expression) {
		this.expression = Objects.requireNonNull(expression);
	}

	@Override
	public <T, E extends Throwable> T accept(StatementVisitor<T, E> visitor) throws E {
		return visitor.visitExpressionStatement(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ExpressionStatement)) {
			return false;
		}
		ExpressionStatement statement = (ExpressionStatement) obj;
		return statement.getSource().equals(getSource())
			&& statement.getExpression().equals(getExpression());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getExpression()
		);
	}
}