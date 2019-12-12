package dev.mamo.craftlangc;

import java.io.*;
import java.util.*;

public class FQN implements Serializable {
	private Namespace namespace;
	private String name;

	private static List<String> nonEmpty(List<String> components) {
		if (components.isEmpty()) {
			throw new IllegalArgumentException("There has to be at least one component");
		}
 		return components;
	}

	public FQN(Namespace namespace, String name) {
		setNamespace(namespace);
		setName(name);
	}

	public FQN(List<String> components) {
		this(nonEmpty(components), components.size() - 1);
	}

	public FQN(String... components) {
		this(Arrays.asList(components));
	}

	private FQN(List<String> components, int lastIndex) {
		this(lastIndex > 0 ? new Namespace(components.subList(0, lastIndex)) : null, components.get(lastIndex));
	}

	public Namespace getNamespace() {
		return namespace;
	}

	public void setNamespace(Namespace namespace) {
		this.namespace = namespace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Objects.requireNonNull(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FQN)) {
			return false;
		}
		FQN FQN = (FQN) obj;
		return Objects.equals(FQN.getNamespace(), getNamespace())
			&& FQN.getName().equals(getName());
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
		return namespace != null ? namespace + "." + getName() : getName();
	}
}