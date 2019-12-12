package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class CommandExpression implements Expression, Serializable {
	private ParseNode source;
	private String command;

	public CommandExpression(ParseNode source, String command) {
		setSource(source);
		setCommand(command);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = Objects.requireNonNull(command);
	}

	@Override
	public <T, E extends Throwable> T accept(ExpressionVisitor<T, E> visitor) throws E {
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