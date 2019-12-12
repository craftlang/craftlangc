package dev.mamo.craftlangc.ast.expression;

import dev.mamo.craftlangc.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.util.*;

public class CallExpression implements Expression, Serializable {
	private ParseNode source;
	private FQN functionFQN;
	private List<Expression> arguments;

	public CallExpression(ParseNode source, FQN functionFQN, List<Expression> arguments) {
		setSource(source);
		setFunctionFQN(functionFQN);
		setArguments(arguments);
	}

	@Override
	public ParseNode getSource() {
		return source;
	}

	@Override
	public void setSource(ParseNode source) {
		this.source = Objects.requireNonNull(source);
	}

	public FQN getFunctionFQN() {
		return functionFQN;
	}

	public void setFunctionFQN(FQN functionFQN) {
		this.functionFQN = Objects.requireNonNull(functionFQN);
	}

	public List<Expression> getArguments() {
		return arguments;
	}

	public void setArguments(List<Expression> arguments) {
		this.arguments = Objects.requireNonNull(arguments);
	}

	@Override
	public <T, E extends Throwable> T accept(ExpressionVisitor<T, E> visitor) throws E {
		return visitor.visitFunctionCallExpression(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CallExpression)) {
			return false;
		}
		CallExpression expression = (CallExpression) obj;
		return expression.getSource().equals(getSource())
			&& expression.getFunctionFQN().equals(getFunctionFQN())
			&& expression.getArguments().equals(getArguments());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			getSource(),
			getFunctionFQN(),
			getArguments()
		);
	}
}