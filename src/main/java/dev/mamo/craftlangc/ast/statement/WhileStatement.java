package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class WhileStatement implements Statement, Serializable {
	private final ParseNode source;
	private final Expression condition;
	private final List<Statement> body;

	public WhileStatement(ParseNode source, Expression condition, List<Statement> body) {
		this.source = Objects.requireNonNull(source);
		this.condition = Objects.requireNonNull(condition);
		this.body = Collections.unmodifiableList(body.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
	}

	public WhileStatement(ParseNode source, Expression condition, Statement... body) {
		this(source, condition, Arrays.asList(body));
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public Expression getCondition() {
		return condition;
	}

	public List<Statement> getBody() {
		return body;
	}

	@Override
	public <T, U extends Throwable> T accept(StatementVisitor<T, U> visitor) throws U {
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