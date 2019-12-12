package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class IfStatement implements Statement, Serializable {
	private ParseNode source;
	private Expression condition;
	private List<Statement> trueBranch;
	private List<Statement> falseBranch;

	public IfStatement(ParseNode source, Expression condition, List<Statement> trueBranch, List<Statement> falseBranch) {
		setSource(source);
		setCondition(condition);
		setTrueBranch(trueBranch);
		setFalseBranch(falseBranch);
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

	public List<Statement> getTrueBranch() {
		return trueBranch;
	}

	public void setTrueBranch(List<Statement> trueBranch) {
		this.trueBranch = Objects.requireNonNull(trueBranch);
	}

	public List<Statement> getFalseBranch() {
		return falseBranch;
	}

	public void setFalseBranch(List<Statement> falseBranch) {
		this.falseBranch = Objects.requireNonNull(falseBranch);
	}

	@Override
	public <T, E extends Throwable> T accept(StatementVisitor<T, E> visitor) throws E {
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
			&& statement.getTrueBranch().equals(getTrueBranch())
			&& statement.getFalseBranch().equals(getFalseBranch());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getCondition(),
			getTrueBranch(),
			getFalseBranch()
		);
	}
}