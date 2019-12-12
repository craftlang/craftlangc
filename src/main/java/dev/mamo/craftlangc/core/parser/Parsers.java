package dev.mamo.craftlangc.core.parser;

import dev.mamo.craftlangc.core.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class Parsers {
	private Parsers() {}

	public static ParseNode parse(Function<ParseContext, ParseNode> parser, ParseContext context) {
		return parser.apply(context);
	}

	public static Function<ParseContext, ParseNode> end() {
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

	public static Function<ParseContext, ParseNode> string(int maxLength, Predicate<String> predicate) {
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

	public static Function<ParseContext, ParseNode> string(String string, boolean ignoreCase) {
		return string(string.length(), ignoreCase ? s -> s.equalsIgnoreCase(string) : s -> s.equals(string));
	}

	public static Function<ParseContext, ParseNode> string(String string) {
		return string(string, false);
	}

	public static Function<ParseContext, ParseNode> character(Predicate<Character> predicate) {
		return string(1, s -> s.length() == 1 && predicate.test(s.charAt(0)));
	}

	public static Function<ParseContext, ParseNode> character(char character) {
		return character(c -> c == character);
	}

	public static Function<ParseContext, ParseNode> range(char min, char max) {
		return character(c -> c >= min && c <= max);
	}

	public static Function<ParseContext, ParseNode> any() {
		return character(c -> true);
	}

	public static Function<ParseContext, ParseNode> repetition(Function<ParseContext, ParseNode> parser, int min, int max) {
		return context -> {
			int initialPosition = context.getPosition();
			List<ParseNode> children = new ArrayList<>();

			while (max < 0 || children.size() < max) {
				ParseNode parsed = parse(parser, context);
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

	public static Function<ParseContext, ParseNode> repetition(Function<ParseContext, ParseNode> parser, int count) {
		return repetition(parser, count, count);
	}

	public static Function<ParseContext, ParseNode> atLeast(Function<ParseContext, ParseNode> parser, int min) {
		return repetition(parser, min, -1);
	}

	public static Function<ParseContext, ParseNode> atMost(Function<ParseContext, ParseNode> parser, int max) {
		return repetition(parser, 0, max);
	}

	public static Function<ParseContext, ParseNode> zeroOrMore(Function<ParseContext, ParseNode> parser) {
		return atLeast(parser, 0);
	}

	public static Function<ParseContext, ParseNode> oneOrMore(Function<ParseContext, ParseNode> parser) {
		return atLeast(parser, 1);
	}

	public static Function<ParseContext, ParseNode> optional(Function<ParseContext, ParseNode> parser) {
		return repetition(parser, 0, 1);
	}

	public static Function<ParseContext, ParseNode> test(Function<ParseContext, ParseNode> parser) {
		return context -> {
			int initialPosition = context.getPosition();
			ParseNode parsed = parse(parser, context);

			if (parsed != null) {
				context.setPosition(initialPosition);
				return new ParseNode(context.getSource(), initialPosition, initialPosition);
			} else {
				context.setError(null);
				return null;
			}
		};
	}

	public static Function<ParseContext, ParseNode> not(Function<ParseContext, ParseNode> parser) {
		return context -> {
			int initialPosition = context.getPosition();
			ParseNode parsed = parse(parser, context);

			if (parsed == null) {
				return new ParseNode(context.getSource(), initialPosition, initialPosition);
			} else {
				context.setPosition(initialPosition);
				context.setError(null);
				return null;
			}
		};
	}

	public static Function<ParseContext, ParseNode> sequence(List<Function<ParseContext, ParseNode>> parsers) {
		List<Function<ParseContext, ParseNode>> ps = parsers.stream().map(Objects::requireNonNull).collect(Collectors.toList());

		return context -> {
			int initialPosition = context.getPosition();
			List<ParseNode> children = new ArrayList<>();

			for (Function<ParseContext, ParseNode> parser : ps) {
				ParseNode parsed = parse(parser, context);
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

	@SafeVarargs
	public static Function<ParseContext, ParseNode> sequence(Function<ParseContext, ParseNode>... parsers) {
		return sequence(Arrays.asList(parsers));
	}

	public static Function<ParseContext, ParseNode> alternative(List<Function<ParseContext, ParseNode>> parsers) {
		List<Function<ParseContext, ParseNode>> ps = parsers.stream().map(Objects::requireNonNull).collect(Collectors.toList());

		return context -> {
			for (Function<ParseContext, ParseNode> parser : ps) {
				ParseNode parsed = parse(parser, context);
				if (parsed != null) {
					return parsed;
				}
			}

			context.setError(null);
			return null;
		};
	}

	@SafeVarargs
	public static Function<ParseContext, ParseNode> alternative(Function<ParseContext, ParseNode>... parsers) {
		return alternative(Arrays.asList(parsers));
	}

	public static Function<ParseContext, ParseNode> labeled(String label, Function<ParseContext, ParseNode> parser) {
		return context -> {
			ParseNode parsed = parse(parser, context);

			if (parsed != null) {
				return new ParseNode(label, parsed.getSource(), parsed.getBeginIndex(), parsed.getEndIndex(), parsed.getChildren());
			} else {
				context.setError("Expected " + label);
				return null;
			}
		};
	}

	public static Function<ParseContext, ParseNode> required(Function<ParseContext, ParseNode> parser, String errorMessage) {
		return context -> {
			ParseNode parsed = parse(parser, context);

			if (parsed != null) {
				return parsed;
			} else {
				context.setError(errorMessage);
				return null;
			}
		};
	}

	public static ForwardFunction<ParseContext, ParseNode> forward() {
		return new ForwardFunction<>();
	}

	public static void setParser(ForwardFunction<ParseContext, ParseNode> forwardParser, Function<ParseContext, ParseNode> parser) {
		forwardParser.setFunction(parser);
	}
}