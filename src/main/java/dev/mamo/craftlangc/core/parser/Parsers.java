package dev.mamo.craftlangc.core.parser;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class Parsers {
	private Parsers() {}

	public static Parser end() {
		return context -> {
			if (context.isAtEnd()) {
				int position = context.getPosition();
				return new ParseNode(context.getSource(), position, position);
			} else {
				context.setError(null);
				return null;
			}
		};
	}

	public static Parser string(int maxLength, Predicate<String> predicate) {
		return context -> {
			int initialPosition = context.getPosition();
			String peek = context.peek(maxLength);

			if (predicate.test(peek)) {
				context.advance(peek.length());
				return new ParseNode(context.getSource(), initialPosition, context.getPosition());
			} else {
				context.setError(null);
				return null;
			}
		};
	}

	public static Parser string(String string, boolean ignoreCase) {
		return string(string.length(), ignoreCase ? s -> s.equalsIgnoreCase(string) : s -> s.equals(string));
	}

	public static Parser string(String string) {
		return string(string, false);
	}

	public static Parser character(Predicate<Character> predicate) {
		return string(1, s -> s.length() == 1 && predicate.test(s.charAt(0)));
	}

	public static Parser character(char character) {
		return character(c -> c == character);
	}

	public static Parser range(char min, char max) {
		return character(c -> c >= min && c <= max);
	}

	public static Parser any() {
		return character(c -> true);
	}

	public static Parser repetition(Parser parser, int min, int max) {
		return context -> {
			int initialPosition = context.getPosition();
			List<ParseNode> children = new ArrayList<>();

			while (max < 0 || children.size() < max) {
				ParseNode parsed = parser.parse(context);
				if (parsed != null) {
					children.add(parsed);
				} else {
					break;
				}
			}

			int childCount = children.size();
			if (childCount >= min && (max < 0 || childCount <= max)) {
				return new ParseNode(context.getSource(), initialPosition, context.getPosition(), children);
			} else {
				context.setPosition(initialPosition);
				context.setError(null);
				return null;
			}
		};
	}

	public static Parser repetition(Parser parser, int count) {
		return repetition(parser, count, count);
	}

	public static Parser atLeast(Parser parser, int min) {
		return repetition(parser, min, -1);
	}

	public static Parser atMost(Parser parser, int max) {
		return repetition(parser, 0, max);
	}

	public static Parser zeroOrMore(Parser parser) {
		return atLeast(parser, 0);
	}

	public static Parser oneOrMore(Parser parser) {
		return atLeast(parser, 1);
	}

	public static Parser optional(Parser parser) {
		return repetition(parser, 0, 1);
	}

	public static Parser test(Parser parser) {
		return context -> {
			int initialPosition = context.getPosition();
			ParseNode parsed = parser.parse(context);

			if (parsed != null) {
				context.setPosition(initialPosition);
				return new ParseNode(context.getSource(), initialPosition, initialPosition);
			} else {
				context.setError(null);
				return null;
			}
		};
	}

	public static Parser not(Parser parser) {
		return context -> {
			int initialPosition = context.getPosition();
			ParseNode parsed = parser.parse(context);

			if (parsed == null) {
				return new ParseNode(context.getSource(), initialPosition, initialPosition);
			} else {
				context.setPosition(initialPosition);
				context.setError(null);
				return null;
			}
		};
	}

	public static Parser sequence(List<Parser> parsers) {
		List<Parser> ps = parsers.stream().map(Objects::requireNonNull).collect(Collectors.toList());

		return context -> {
			int initialPosition = context.getPosition();
			List<ParseNode> children = new ArrayList<>();

			for (Parser parser : ps) {
				ParseNode parsed = parser.parse(context);
				if (parsed != null) {
					children.add(parsed);
				} else {
					context.setPosition(initialPosition);
					context.setError(null);
					return null;
				}
			}

			return new ParseNode(context.getSource(), initialPosition, context.getPosition(), children);
		};
	}

	public static Parser sequence(Parser... parsers) {
		return sequence(Arrays.asList(parsers));
	}

	public static Parser alternative(List<Parser> parsers) {
		List<Parser> ps = parsers.stream().map(Objects::requireNonNull).collect(Collectors.toList());

		return context -> {
			for (Parser parser : ps) {
				ParseNode parsed = parser.parse(context);
				if (parsed != null) {
					return parsed;
				}
			}

			context.setError(null);
			return null;
		};
	}

	public static Parser alternative(Parser... parsers) {
		return alternative(Arrays.asList(parsers));
	}

	public static Parser labeled(String label, Parser parser) {
		return context -> {
			ParseNode parsed = parser.parse(context);

			if (parsed != null) {
				return new ParseNode(label, parsed.getSource(), parsed.getBeginIndex(), parsed.getEndIndex(), parsed.getChildren());
			} else {
				context.setError("Expected " + label);
				return null;
			}
		};
	}

	public static Parser required(Parser parser, String errorMessage) {
		return context -> {
			ParseNode parsed = parser.parse(context);

			if (parsed != null) {
				return parsed;
			} else {
				context.setError(errorMessage);
				return null;
			}
		};
	}

	public static ForwardParser forward() {
		return new ForwardParser();
	}

	public static class ForwardParser implements Parser {
		private Parser parser = null;

		private ForwardParser() {
		}

		public Parser getParser() {
			return parser;
		}

		public void setParser(Parser parser) {
			this.parser = parser;
		}

		@Override
		public ParseNode parse(ParseContext context) {
			return getParser().parse(context);
		}
	}
}