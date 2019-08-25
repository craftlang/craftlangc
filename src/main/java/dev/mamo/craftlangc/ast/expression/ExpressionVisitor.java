package dev.mamo.craftlangc.ast.expression;

public interface ExpressionVisitor<T, U extends Throwable> {
	default T visitBinaryExpression(BinaryExpression expression) throws U {
		return null;
	}

	default T visitUnaryExpression(UnaryExpression expression) throws U {
		return null;
	}

	default T visitIntegerExpression(IntegerExpression expression) throws U {
		return null;
	}

	default T visitCommandExpression(CommandExpression expression) throws U {
		return null;
	}

	default T visitFunctionCallExpression(FunctionCallExpression expression) throws U {
		return null;
	}

	default T visitVariableExpression(VariableExpression expression) throws U {
		return null;
	}
}