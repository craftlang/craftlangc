package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.ast.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class Argument implements Node, Serializable {
	private final ParseNode source;
	private final boolean byReference;
	private final Expression expression;

	public Argument(ParseNode source, boolean byReference, Expression expression) {
		this.source = Objects.requireNonNull(source);
		this.byReference = byReference;
		this.expression = Objects.requireNonNull(expression);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public boolean isByReference() {
		return byReference;
	}

	public Expression getExpression() {
		return expression;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Argument)) {
			return false;
		}
		Argument argument = (Argument) obj;
		return argument.getSource().equals(getSource())
			&& argument.isByReference() == isByReference()
			&& argument.getExpression().equals(getExpression());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			isByReference(),
			getExpression()
		);
	}
}