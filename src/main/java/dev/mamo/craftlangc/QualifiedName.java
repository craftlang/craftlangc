package dev.mamo.craftlangc;

import java.io.*;
import java.util.*;

public class QualifiedName implements Serializable {
	private final Namespace namespace;
	private final String name;

	public QualifiedName(Namespace namespace, String name) {
		this.namespace = namespace;
		this.name = Objects.requireNonNull(name);
	}

	public QualifiedName(String name) {
		this(null, name);
	}

	public Namespace getNamespace() {
		return namespace;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof QualifiedName)) {
			return false;
		}
		QualifiedName qualifiedName = (QualifiedName) obj;
		return Objects.equals(qualifiedName.getNamespace(), getNamespace())
			&& qualifiedName.getName().equals(getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getNamespace(),
			getName()
		);
	}

	@Override
	public String toString() {
		Namespace namespace = getNamespace();
		return namespace != null ? namespace + getName() : getName();
	}
}