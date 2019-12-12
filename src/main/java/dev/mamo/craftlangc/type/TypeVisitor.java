package dev.mamo.craftlangc.type;

public interface TypeVisitor<T, E extends Throwable> {
	default T visitPrimitiveType(PrimitiveType type) throws E {
		return null;
	}

	default T visitCompoundType(CompoundType type) throws E {
		return null;
	}
}