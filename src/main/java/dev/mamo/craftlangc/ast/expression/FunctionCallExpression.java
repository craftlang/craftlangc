package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class FunctionCallExpression implements Expression, Serializable {
	private final ParseNode source;
	private final QualifiedName functionName;
	private final List<Argument> arguments;

	public FunctionCallExpression(ParseNode source, QualifiedName functionName, List<Argument> arguments) {
		this.source = Objects.requireNonNull(source);
		this.functionName = Objects.requireNonNull(functionName);
		this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
	}

	public FunctionCallExpression(ParseNode source, QualifiedName functionName, Argument... arguments) {
		this(source, functionName, Arrays.asList(arguments));
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	public QualifiedName getFunctionName() {
		return functionName;
	}

	public List<Argument> getArguments() {
		return arguments;
	}

	@Override
	public <T, U extends Throwable> T accept(ExpressionVisitor<T, U> visitor) throws U {
		return visitor.visitFunctionCallExpression(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FunctionCallExpression)) {
			return false;
		}
		FunctionCallExpression expression = (FunctionCallExpression) obj;
		return expression.getSource().equals(getSource())
			&& expression.getFunctionName().equals(getFunctionName())
			&& expression.getArguments().equals(getArguments());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getFunctionName(),
			getArguments()
		);
	}
}