package dev.mamo.craftlangc;

import java.io.*;
import java.util.*;

public class Namespace implements Serializable {
	private List<String> components;

	public Namespace(List<String> components) {
		setComponents(components);
	}

	public Namespace(String... components) {
		this(Arrays.asList(components));
	}

	public List<String> getComponents() {
		return components;
	}

	public void setComponents(List<String> components) {
		this.components = Objects.requireNonNull(components);
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