package dev.mamo.craftlangc.type;

import java.io.*;

public enum PrimitiveType implements Type, Serializable {
	BOOLEAN,
	INTEGER;

	@Override
	public boolean isPrimitive() {
		return true;
	}

	@Override
	public boolean isRecursive() {
		return false;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public <T, E extends Throwable> T accept(TypeVisitor<T, E> visitor) throws E {
		return visitor.visitPrimitiveType(this);
	}
}