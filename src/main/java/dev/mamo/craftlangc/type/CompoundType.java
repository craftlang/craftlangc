package dev.mamo.craftlangc.type;

import java.io.*;
import java.util.*;
import java.util.Map.*;

public class CompoundType implements Type, Serializable {
	private final Map<String, Type> members;
	private final TypeVisitor<Boolean, RuntimeException> VISITOR = new TypeVisitor<Boolean, RuntimeException>() {
		@Override
		public Boolean visitPrimitiveType(PrimitiveType type) {
			return false;
		}

		@Override
		public Boolean visitCompoundType(CompoundType type) {
			for (Type member : type.getMembers().values()) {
				if (member == CompoundType.this || member.accept(this)) {
					return true;
				}
			}
			return false;
		}
	};

	public CompoundType(Map<String, Type> members) {
		for (Entry<String, Type> entry : members.entrySet()) {
			Objects.requireNonNull(entry.getKey());
			Objects.requireNonNull(entry.getValue());
		}
		this.members = Collections.unmodifiableMap(new LinkedHashMap<>(members));
	}

	public Map<String, Type> getMembers() {
		return members;
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isRecursive() {
		for (Type member : getMembers().values()) {
			if (member.accept(VISITOR)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int size() {
		return getMembers().size();
	}

	@Override
	public <T, E extends Throwable> T accept(TypeVisitor<T, E> visitor) throws E {
		return visitor.visitCompoundType(this);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CompoundType && ((CompoundType) obj).getMembers().equals(getMembers());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getMembers());
	}
}