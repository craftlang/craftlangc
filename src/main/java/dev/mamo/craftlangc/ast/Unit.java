package dev.mamo.craftlangc.ast;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.ast.statement.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Unit implements Node, Serializable {
	private final ParseNode source;
	private final Namespace namespace;
	private final List<VariableDeclarationStatement> variableDeclarations;
	private final List<FunctionDefinition> functions;

	public Unit(ParseNode source, Namespace namespace, List<VariableDeclarationStatement> variableDeclarations, List<FunctionDefinition> functions) {
		this.source = Objects.requireNonNull(source);
		this.namespace = namespace != null ? namespace : new Namespace("minecraft");
		this.variableDeclarations = Collections.unmodifiableList(variableDeclarations.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
		this.functions = Collections.unmodifiableList(functions.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
	}

	public Unit(ParseNode source, Namespace namespace, VariableDeclarationStatement[] variableDeclarations, FunctionDefinition[] functions) {
		this(source, namespace, Arrays.asList(variableDeclarations), Arrays.asList(functions));
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public Namespace getNamespace() {
		return namespace;
	}

	public List<VariableDeclarationStatement> getVariableDeclarations() {
		return variableDeclarations;
	}

	public List<FunctionDefinition> getFunctions() {
		return functions;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Unit)) {
			return false;
		}
		Unit unit = (Unit) obj;
		return unit.getSource().equals(getSource())
			&& unit.getNamespace().equals(getNamespace())
			&& unit.getVariableDeclarations().equals(getVariableDeclarations())
			&& unit.getFunctions().equals(getFunctions());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getNamespace(),
			getVariableDeclarations(),
			getFunctions()
		);
	}
}