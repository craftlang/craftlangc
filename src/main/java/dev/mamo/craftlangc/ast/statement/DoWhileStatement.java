package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class DoWhileStatement implements Statement, Serializable {
	private final ParseNode source;
	private final List<Statement> body;
	private final Expression condition;

	public DoWhileStatement(ParseNode source, List<Statement> body, Expression condition) {
		this.source = Objects.requireNonNull(source);
		this.body = Collections.unmodifiableList(body.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
		this.condition = Objects.requireNonNull(condition);
	}

	public DoWhileStatement(ParseNode source, Statement[] body, Expression condition) {
		this(source, Arrays.asList(body), condition);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public List<Statement> getBody() {
		return body;
	}

	public Expression getCondition() {
		return condition;
	}

	@Override
	public <T, U extends Throwable> T accept(StatementVisitor<T, U> visitor) throws U {
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