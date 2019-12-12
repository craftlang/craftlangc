package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class DoWhileStatement implements Statement, Serializable {
	private ParseNode source;
	private List<Statement> body;
	private Expression condition;

	public DoWhileStatement(ParseNode source, List<Statement> body, Expression condition) {
		setSource(source);
		setBody(body);
		setCondition(condition);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public List<Statement> getBody() {
		return body;
	}

	public void setBody(List<Statement> body) {
		this.body = Objects.requireNonNull(body);
	}

	public Expression getCondition() {
		return condition;
	}

	public void setCondition(Expression condition) {
		this.condition = Objects.requireNonNull(condition);
	}

	@Override
	public <T, E extends Throwable> T accept(StatementVisitor<T, E> visitor) throws E {
		return visitor.visitDoWhileStatement(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DoWhileStatement)) {
			return false;
		}
		DoWhileStatement statement = (DoWhileStatement) obj;
		return statement.getSource().equals(getSource())
			&& statement.getBody().equals(getBody())
			&& statement.getCondition().equals(getCondition());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSource(),
			getBody(),
			getCondition()
		);
	}
}