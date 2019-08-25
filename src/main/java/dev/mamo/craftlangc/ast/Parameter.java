package dev.mamo.craftlangc.ast;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class Parameter implements Node, Serializable {
	private final ParseNode source;
	private final Type type;
	private final String name;

	public Parameter(ParseNode source, Type type, String name) {
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
	public boolean equals(Object obj) {
		if (!(obj instanceof Parameter)) {
			return false;
		}
		Parameter parameter = (Parameter) obj;
		return parameter.getSource().equals(getSource())
			&& parameter.getType().equals(getType())
			&& parameter.getName().equals(getName());
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