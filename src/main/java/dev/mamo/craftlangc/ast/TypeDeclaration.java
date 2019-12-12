package dev.mamo.craftlangc.ast;

import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class TypeDeclaration implements Node, Serializable {
	private ParseNode source;
	private String name;
	private List<TypeAndName> members;

	public TypeDeclaration(ParseNode source, String name, List<TypeAndName> members) {
		this.source = Objects.requireNonNull(source);
		this.name = Objects.requireNonNull(name);
		this.members = Collections.unmodifiableList(members.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Objects.requireNonNull(name);
	}

	public List<TypeAndName> getMembers() {
		return members;
	}

	public void setMembers(List<TypeAndName> members) {
		this.members = Objects.requireNonNull(members);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TypeDeclaration)) {
			return false;
		}
		TypeDeclaration typeDeclaration = (TypeDeclaration) obj;
		return typeDeclaration.getSource().equals(getSource())
			&& typeDeclaration.getName().equals(getName())
			&& typeDeclaration.getMembers().equals(getMembers());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getName(),
			getMembers()
		);
	}
}