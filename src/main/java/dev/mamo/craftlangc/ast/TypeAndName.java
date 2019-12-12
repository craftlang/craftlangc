package dev.mamo.craftlangc.ast;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class TypeAndName implements Node, Serializable {
	private ParseNode source;
	private FQN typeFQN;
	private String name;

	public TypeAndName(ParseNode source, FQN typeFQN, String name) {
		setSource(source);
		setTypeFQN(typeFQN);
		setName(name);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public FQN getTypeFQN() {
		return typeFQN;
	}

	public void setTypeFQN(FQN typeFQN) {
		this.typeFQN = Objects.requireNonNull(typeFQN);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Objects.requireNonNull(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TypeAndName)) {
			return false;
		}
		TypeAndName typeAndName = (TypeAndName) obj;
		return typeAndName.getSource().equals(getSource())
			&& typeAndName.getTypeFQN().equals(getTypeFQN())
			&& typeAndName.getName().equals(getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getTypeFQN(),
			getName()
		);
	}
}