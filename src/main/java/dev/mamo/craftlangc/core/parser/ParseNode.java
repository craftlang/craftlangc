package dev.mamo.craftlangc.core.parser;

import dev.mamo.craftlangc.core.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class ParseNode {
	private static final String NL = System.lineSeparator();
	private static final String TAB = "\t";

	private final String label;
	private final String source;
	private final int beginIndex;
	private final int endIndex;
	private final List<ParseNode> children;

	public ParseNode(String label, String source, int beginIndex, int endIndex, List<ParseNode> children) {
		source.substring(beginIndex, endIndex); // Throws exception if arguments are invalid
		this.label = label;
		this.source = Objects.requireNonNull(source);
		this.beginIndex = beginIndex;
		this.endIndex = endIndex;
		this.children = Collections.unmodifiableList(children.stream().map(Objects::requireNonNull).collect(Collectors.toList()));
	}

	public ParseNode(String source, int beginIndex, int endIndex, List<ParseNode> children) {
		this(null, source, beginIndex, endIndex, children);
	}

	public ParseNode(String label, String source, int beginIndex, int endIndex, ParseNode... children) {
		this(label, source, beginIndex, endIndex, Arrays.asList(children));
	}

	public ParseNode(String source, int beginIndex, int endIndex, ParseNode... children) {
		this(null, source, beginIndex, endIndex, children);
	}

	public String getLabel() {
		return label;
	}

	public String getSource() {
		return source;
	}

	public int getBeginIndex() {
		return beginIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public int getLength() {
		return getEndIndex() - getBeginIndex();
	}

	public String getContent() {
		return getSource().substring(getBeginIndex(), getEndIndex());
	}

	public List<ParseNode> getChildren() {
		return children;
	}

	public ParseNode flatten(boolean recursive) {
		List<ParseNode> children = new ArrayList<>();

		for (ParseNode child : getChildren()) {
			if (recursive) {
				child = child.flatten(true);
			}

			if (child.getLabel() != null) {
				children.add(child);
			} else {
				children.addAll(child.getChildren());
			}
		}

		return new ParseNode(getLabel(), getSource(), getBeginIndex(), getEndIndex(), children);
	}

	public ParseNode flatten() {
		return flatten(true);
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof ParseNode)) {
			return false;
		}
		ParseNode token = (ParseNode) obj;
		return Objects.equals(token.getLabel(), getLabel())
			&& token.getSource().equals(getSource())
			&& token.getBeginIndex() == getBeginIndex()
			&& token.getEndIndex() == getEndIndex()
			&& token.getChildren().equals(getChildren());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getLabel(),
			getSource(),
			getBeginIndex(),
			getEndIndex(),
			getChildren()
		);
	}

	public String toString(boolean wide) {
		StringBuilder indent = new StringBuilder();
		StringBuilder result = new StringBuilder();

		(new Consumer<ParseNode>() {
			@Override
			public void accept(ParseNode node) {
				String label = node.getLabel();
				List<ParseNode> children = node.getChildren();
				int childCount = children.size();

				result.append(indent).append('(');
				if (label != null) {
					result.append(label);
				}

				if (childCount == 0) {
					String content = node.getContent();
					if (!content.isEmpty()) {
						if (label != null) {
							result.append(' ');
						}
						result.append(Util.quote(content));
					}
				} else {
					if (wide) {
						indent.append(TAB);
						result.append(NL);
					} else if (label != null) {
						result.append(' ');
					}
					accept(children.get(0));

					for (int i = 1; i < childCount; i++) {
						if (wide) {
							result.append(NL);
						} else {
							result.append(' ');
						}
						accept(children.get(i));
					}

					if (wide) {
						indent.deleteCharAt(indent.length() - 1);
						result.append(NL).append(indent);
					}
				}

				result.append(')');
			}
		}).accept(this);

		return result.toString();
	}

	@Override
	public String toString() {
		return toString(false);
	}
}