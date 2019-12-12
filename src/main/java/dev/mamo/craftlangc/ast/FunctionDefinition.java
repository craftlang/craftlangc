package dev.mamo.craftlangc.ast;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.ast.statement.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class FunctionDefinition implements Node, Serializable {
	private ParseNode source;
	private List<FQN> tags;
	private FQN returnTypeFQN;
	private String name;
	private List<TypeAndName> parameters;
	private List<Statement> body;

	public FunctionDefinition(ParseNode source, List<FQN> tags, FQN returnTypeFQN, String name, List<TypeAndName> parameters, List<Statement> body) {
		setSource(source);
		setTags(tags);
		setReturnTypeFQN(returnTypeFQN);
		setName(name);
		setParameters(parameters);
		setBody(body);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public List<FQN> getTags() {
		return tags;
	}

	public void setTags(List<FQN> tags) {
		this.tags = Objects.requireNonNull(tags);
	}

	public FQN getReturnTypeFQN() {
		return returnTypeFQN;
	}

	public void setReturnTypeFQN(FQN returnTypeFQN) {
		this.returnTypeFQN = returnTypeFQN;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Objects.requireNonNull(name);
	}

	public List<TypeAndName> getParameters() {
		return parameters;
	}

	public void setParameters(List<TypeAndName> parameters) {
		this.parameters = Objects.requireNonNull(parameters);
	}

	public List<Statement> getBody() {
		return body;
	}

	public void setBody(List<Statement> body) {
		this.body = Objects.requireNonNull(body);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FunctionDefinition)) {
			return false;
		}
		FunctionDefinition functionDefinition = (FunctionDefinition) obj;
		return functionDefinition.getSource().equals(getSource())
			&& functionDefinition.getTags().equals(getTags())
			&& Objects.equals(functionDefinition.getReturnTypeFQN(), getReturnTypeFQN())
			&& functionDefinition.getName().equals(getName())
			&& functionDefinition.getParameters().equals(getParameters())
			&& functionDefinition.getBody().equals(getBody());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getTags(),
			getReturnTypeFQN(),
			getName(),
			getParameters(),
			getBody()
		);
	}
}