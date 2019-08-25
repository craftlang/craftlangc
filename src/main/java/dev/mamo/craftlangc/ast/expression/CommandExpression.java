package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class CommandExpression implements Expression, Serializable {
	private final ParseNode source;
	private final String command;

	public CommandExpression(ParseNode source, String command) {
		this.source = Objects.requireNonNull(source);
		this.command = Objects.requireNonNull(command);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public String getCommand() {
		return command;
	}

	@Override
	public <T, U extends Throwable> T accept(ExpressionVisitor<T, U> visitor) throws U {
		return visitor.visitCommandExpression(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CommandExpression)) {
			return false;
		}
		CommandExpression expression = (CommandExpression) obj;
		return expression.getSource().equals(getSource())
			&& expression.getCommand().equals(getCommand());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getCommand()
		);
	}
}