package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class ExpressionStatement implements Statement, Serializable {
	private final ParseNode source;
	private final Expression expression;

	public ExpressionStatement(ParseNode source, Expression expression) {
		this.source = Objects.requireNonNull(source);
		this.expression = Objects.requireNonNull(expression);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public Expression getExpression() {
		return expression;
	}

	@Override
	public <T, U extends Throwable> T accept(StatementVisitor<T, U> visitor) throws U {
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