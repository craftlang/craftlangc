package dev.mamo.craftlangc.type;

public interface Type {
	boolean isPrimitive();

	boolean isRecursive();

	int size();

	<T, E extends Throwable> T accept(TypeVisitor<T, E> visitor) throws E;
}