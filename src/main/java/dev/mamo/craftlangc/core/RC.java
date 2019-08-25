package dev.mamo.craftlangc.core;

import java.io.*;
import java.util.*;

public class RC implements Serializable {
	private final int row;
	private final int column;

	RC(int row, int column) {
		this.row = row;
		this.column = column;
	}

	public int getRow() {
		return row;
	}

	public int getColumn() {
		return column;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RC)) {
			return false;
		}
		RC position = (RC) obj;
		return position.getRow() == getRow()
			&& position.getColumn() == getColumn();
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getRow(),
			getColumn()
		);
	}

	@Override
	public String toString() {
		return getRow() + ":" + getColumn();
	}
}