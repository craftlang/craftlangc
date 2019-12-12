package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class WhileStatement implements Statement, Serializable {
	private ParseNode source;
	private Expression condition;
	private List<Statement> body;

	public WhileStatement(ParseNode source, Expression condition, List<Statement> body) {
		setSource(source);
		setCondition(condition);
		setBody(body);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public Expression getCondition() {
		return condition;
	}

	public void setCondition(Expression condition) {
		this.condition = Objects.requireNonNull(condition);
	}

	public List<Statement> getBody() {
		return body;
	}

	public void setBody(List<Statement> body) {
		this.body = Objects.requireNonNull(body);
	}

	@Override
	public <T, E extends Throwable> T accept(StatementVisitor<T, E> visitor) throws E {
		return visitor.visitWhileStatement(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof WhileStatement)) {
			return false;
		}
		WhileStatement statement = (WhileStatement) obj;
		return statement.getSource().equals(getSource())
			&& statement.getCondition().equals(getCondition())
			&& statement.getBody().equals(getBody());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSource(),
			getCondition(),
			getBody()
		);
	}
}