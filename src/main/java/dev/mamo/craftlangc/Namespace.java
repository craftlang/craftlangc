package dev.mamo.craftlangc;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Namespace implements Serializable {
	private final List<String> components;

	public Namespace(List<String> components) {
		if (components.isEmpty()) {
			throw new IllegalArgumentException("The components must not be empty");
		}
		this.components = Collections.unmodifiableList(components.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
	}

	public Namespace(String... components) {
		this(Arrays.asList(components));
	}

	public List<String> getComponents() {
		return components;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Namespace && ((Namespace) obj).getComponents().equals(getComponents());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getComponents());
	}

	@Override
	public String toString() {
		return String.join(".", getComponents());
	}
}