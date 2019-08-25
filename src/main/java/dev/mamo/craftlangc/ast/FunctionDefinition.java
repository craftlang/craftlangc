package dev.mamo.craftlangc.ast;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.ast.statement.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class FunctionDefinition implements Node, Serializable {
	private final ParseNode source;
	private final List<QualifiedName> tags;
	private final Type returnType;
	private final String name;
	private final List<Parameter> parameters;
	private final List<Statement> body;

	public FunctionDefinition(ParseNode source, List<QualifiedName> tags, Type returnType, String name, List<Parameter> parameters, List<Statement> body) {
		this.source = Objects.requireNonNull(source);
		this.tags = Collections.unmodifiableList(tags.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
		this.returnType = returnType;
		this.name = Objects.requireNonNull(name);
		this.parameters = Collections.unmodifiableList(parameters.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
		this.body = Collections.unmodifiableList(body.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
	}

	public FunctionDefinition(ParseNode source, QualifiedName[] tags, Type returnType, String name, Parameter[] parameters, Statement[] body) {
		this(source, Arrays.asList(tags), returnType, name, Arrays.asList(parameters), Arrays.asList(body));
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public List<QualifiedName> getTags() {
		return tags;
	}

	public Type getReturnType() {
		return returnType;
	}

	public String getName() {
		return name;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public List<Statement> getBody() {
		return body;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FunctionDefinition)) {
			return false;
		}
		FunctionDefinition functionDefinition = (FunctionDefinition) obj;
		return functionDefinition.getSource().equals(getSource())
			&& functionDefinition.getTags().equals(getTags())
			&& Objects.equals(functionDefinition.getReturnType(), getReturnType())
			&& functionDefinition.getName().equals(getName())
			&& functionDefinition.getParameters().equals(getParameters())
			&& functionDefinition.getBody().equals(getBody());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSource(),
			getTags(),
			getReturnType(),
			getName(),
			getParameters(),
			getBody()
		);
	}
}