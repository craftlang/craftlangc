package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class IfStatement implements Statement, Serializable {
	private final ParseNode source;
	private final Expression condition;
	private final List<Statement> ifTrue;
	private final List<Statement> ifFalse;

	public IfStatement(ParseNode source, Expression condition, List<Statement> ifTrue, List<Statement> ifFalse) {
		this.source = Objects.requireNonNull(source);
		this.condition = Objects.requireNonNull(condition);
		this.ifTrue = Collections.unmodifiableList(ifTrue.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
		this.ifFalse = Collections.unmodifiableList(ifFalse.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
	}

	public IfStatement(ParseNode source, Expression condition, Statement[] ifTrue, Statement[] ifFalse) {
		this(source, condition, Arrays.asList(ifTrue), Arrays.asList(ifFalse));
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public Expression getCondition() {
		return condition;
	}

	public List<Statement> getIfTrue() {
		return ifTrue;
	}

	public List<Statement> getIfFalse() {
		return ifFalse;
	}

	@Override
	public <T, U extends Throwable> T accept(StatementVisitor<T, U> visitor) throws U {
		return visitor.visitIfStatement(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IfStatement)) {
			return false;
		}
		IfStatement statement = (IfStatement) obj;
		return statement.getSource().equals(getSource())
			&& statement.getCondition().equals(getCondition())
			&& statement.getIfTrue().equals(getIfTrue())
			&& statement.getIfFalse().equals(getIfFalse());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getCondition(),
			getIfTrue(),
			getIfFalse()
		);
	}
}