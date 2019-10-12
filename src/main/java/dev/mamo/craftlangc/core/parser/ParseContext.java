package dev.mamo.craftlangc.core.parser;

import java.io.*;
import java.util.*;

public class ParseContext implements Serializable {
	private final String source;
	private int position = 0;
	private Error furthestError = null;

	public ParseContext(String source) {
		this.source = Objects.requireNonNull(source);
	}

	public String getSource() {
		return source;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = Math.max(0, Math.min(position, getSource().length()));
	}

	public boolean isAtEnd() {
		return getPosition() == getSource().length();
	}

	public String peek(int maxOffset) {
		if (maxOffset <= 0) {
			return "";
		}

		String source = getSource();
		int position = getPosition();
		return source.substring(position, Math.min(position + maxOffset, source.length()));
	}

	public String advance(int maxOffset) {
		if (maxOffset <= 0) {
			return "";
		}

		String source = getSource();
		int position = getPosition();
		int newPosition = Math.min(position + maxOffset, source.length());
		String result = source.substring(position, newPosition);
		setPosition(newPosition);
		return result;
	}

	public Error getFurthestError() {
		return furthestError;
	}

	public void setError(String message) {
		if (furthestError == null) {
			furthestError = new Error(getPosition(), message);
		} else {
			if (furthestError.getMessage() != null) {
				int position;
				if (message != null && furthestError.getPosition() < (position = getPosition())) {
					furthestError = new Error(position, message);
				}
			} else {
				int position = getPosition();
				if (message != null || furthestError.getPosition() < position) {
					furthestError = new Error(position, message);
				}
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ParseContext)) {
			return false;
		}
		ParseContext context = (ParseContext) obj;
		return context.getSource().equals(getSource())
			&& context.getPosition() == getPosition();
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getPosition()
		);
	}

	public class Error implements Serializable {
		private final int position;
		private final String message;

		private Error(int position, String message) {
			this.position = position;
			this.message = message;
		}

		public int getPosition() {
			return position;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Error)) {
				return false;
			}
			Error error = (Error) obj;
			return getParseContext().equals(error.getParseContext())
				&& error.getPosition() == getPosition()
				&& Objects.equals(error.getMessage(), getMessage());
		}

		@Override
		public int hashCode() {
			return Objects.hash(
				getParseContext(),
				getPosition(),
				getMessage()
			);
		}

		private ParseContext getParseContext() {
			return ParseContext.this;
		}
	}
}