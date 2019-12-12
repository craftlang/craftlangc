package dev.mamo.craftlangc.core;

import java.io.*;
import java.util.*;

public class Scope<K, V> implements Serializable {
	private final Map<K, V> env = new HashMap<>();
	private Scope<K, V> parent;

	public Scope(Scope<K, V> parent) {
		setParent(parent);
	}

	public Scope() {
		this(null);
	}

	public Scope<K, V> getParent() {
		return parent;
	}

	public void setParent(Scope<K, V> parent) {
		this.parent = parent;
	}

	public Scope<K, V> getRoot() {
		Scope root = this;
		Scope parent;
		while ((parent = root.getParent()) != null) {
			root = parent;
		}
		return root;
	}

	public Scope<K, V> getRoot(K key) {
		Scope<K, V> scope = this;
		while (scope != null && !scope.env.containsKey(key)) {
			scope = scope.getParent();
		}
		return scope;
	}

	public V get(K key) {
		Scope<K, V> scope = getRoot(key);
		return scope != null ? scope.env.get(key) : null;
	}

	public boolean isDefined(K key) {
		return getRoot(key) != null;
	}

	public void define(K key, V value) {
		env.put(key, value);
	}

	public void assign(K key, V value) {
		Scope<K, V> scope = this;
		while (scope != null && !scope.env.containsKey(key)) {
			scope = scope.getParent();
		}
		if (scope == null) {
			throw new IllegalStateException("Undeclared " + key);
		}
		scope.env.put(key, value);
	}

	public int size() {
		int size = env.size();
		Scope<K, V> parent = getParent();
		if (parent != null) {
			size += parent.size();
		}
		return size;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Scope)) {
			return false;
		}
		Scope scope = (Scope) obj;
		return Objects.equals(scope.getParent(), getParent())
			&& scope.env.equals(env);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getParent(),
			env
		);
	}
}