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

	public V get(K key) {
		Scope<K, V> scope = this;
		while (scope != null && !scope.env.containsKey(key)) {
			scope = scope.getParent();
		}
		return scope != null ? scope.env.get(key) : null;
	}

	public void declare(K key) {
		declareAndAssign(key, null);
	}

	public void declareAndAssign(K key, V value) {
		env.put(key, value);
	}

	public void assign(K key, V value) {
		Scope<K, V> scope = this;
		while (scope != null && !scope.env.containsKey(key)) {
			scope = scope.getParent();
		}
		if (scope != null) {
			scope.env.put(key, value);
		}
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