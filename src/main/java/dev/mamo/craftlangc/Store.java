package dev.mamo.craftlangc;

import dev.mamo.craftlangc.type.*;

import java.io.*;
import java.util.*;

public class Store implements Serializable {
	private Type type;
	private int address;

	public Store(Type type, int address) {
		setType(type);
		setAddress(address);
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = Objects.requireNonNull(type);
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Store)) {
			return false;
		}
		Store store = (Store) obj;
		return store.getType().equals(getType())
			&& store.getAddress() == getAddress();
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getType(),
			getAddress()
		);
	}
}