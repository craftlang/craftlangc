package dev.mamo.craftlangc.core;

import java.util.*;
import java.util.function.*;

public class ForwardFunction<T, R> implements Function<T, R> {
	private Function<T, R> function = null;

	public void setFunction(Function<T, R> function) {
		if (this.function != null) {
			throw new IllegalStateException("Function already set");
		}
		this.function = Objects.requireNonNull(function);
	}

	@Override
	public R apply(T t) {
		if (function == null) {
			throw new IllegalStateException("Function not set");
		}
		return function.apply(t);
	}
}