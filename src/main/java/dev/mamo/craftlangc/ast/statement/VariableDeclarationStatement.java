package dev.mamo.craftlangc.ast.statement;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class VariableDeclarationStatement implements Statement, Serializable {
	private final ParseNode source;
	private final Type type;
	private final String name;

	public VariableDeclarationStatement(ParseNode source, Type type, String name) {
		this.source = Objects.requireNonNull(source);
		this.type = Objects.requireNonNull(type);
		this.name = Objects.requireNonNull(name);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	@Override
	public <T, U extends Throwable> T accept(StatementVisitor<T, U> visitor) throws U {
		return visitor.visitVariableDeclarationStatement(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof VariableDeclarationStatement)) {
			return false;
		}
		VariableDeclarationStatement statement = (VariableDeclarationStatement) obj;
		return statement.getSource().equals(getSource())
			&& statement.getType().equals(getType())
			&& statement.getName().equals(getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getType(),
			getName()
		);
	}
}