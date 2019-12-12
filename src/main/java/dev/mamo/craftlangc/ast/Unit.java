package dev.mamo.craftlangc.ast;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.ast.statement.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class Unit implements Node, Serializable {
	private ParseNode source;
	private Namespace namespace;
	private List<TypeDeclaration> typeDeclarations;
	private List<VariableDeclarationStatement> variableDeclarations;
	private List<FunctionDefinition> functionDefinitions;

	public Unit(ParseNode source, Namespace namespace, List<TypeDeclaration> typeDeclarations, List<VariableDeclarationStatement> variableDeclarations, List<FunctionDefinition> functionDefinitions) {
		setSource(source);
		setNamespace(namespace);
		setTypeDeclarations(typeDeclarations);
		setVariableDeclarations(variableDeclarations);
		setFunctionDefinitions(functionDefinitions);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public Namespace getNamespace() {
		return namespace;
	}

	public void setNamespace(Namespace namespace) {
		this.namespace = Objects.requireNonNull(namespace);
	}

	public List<TypeDeclaration> getTypeDeclarations() {
		return typeDeclarations;
	}

	public void setTypeDeclarations(List<TypeDeclaration> typeDeclarations) {
		this.typeDeclarations = Objects.requireNonNull(typeDeclarations);
	}

	public List<VariableDeclarationStatement> getVariableDeclarations() {
		return variableDeclarations;
	}

	public void setVariableDeclarations(List<VariableDeclarationStatement> variableDeclarations) {
		this.variableDeclarations = Objects.requireNonNull(variableDeclarations);
	}

	public List<FunctionDefinition> getFunctionDefinitions() {
		return functionDefinitions;
	}

	public void setFunctionDefinitions(List<FunctionDefinition> functionDefinitions) {
		this.functionDefinitions = Objects.requireNonNull(functionDefinitions);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Unit)) {
			return false;
		}
		Unit unit = (Unit) obj;
		return unit.getSource().equals(getSource())
			&& unit.getNamespace().equals(getNamespace())
			&& unit.getTypeDeclarations().equals(getTypeDeclarations())
			&& unit.getVariableDeclarations().equals(getVariableDeclarations())
			&& unit.getFunctionDefinitions().equals(getFunctionDefinitions());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getNamespace(),
			getTypeDeclarations(),
			getVariableDeclarations(),
			getFunctionDefinitions()
		);
	}
}