package dev.mamo.craftlangc.ast.expression;

public interface ExpressionVisitor<T, E extends Throwable> {
	default T visitBinaryExpression(BinaryExpression expression) throws E {
		return null;
	}

	default T visitUnaryExpression(UnaryExpression expression) throws E {
		return null;
	}

	default T visitIntegerExpression(IntegerExpression expression) throws E {
		return null;
	}

	default T visitCommandExpression(CommandExpression expression) throws E {
		return null;
	}

	default T visitFunctionCallExpression(CallExpression expression) throws E {
		return null;
	}

	default T visitVariableExpression(VariableExpression expression) throws E {
		return null;
	}
}