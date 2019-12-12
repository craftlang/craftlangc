package dev.mamo.craftlangc.ast.statement;

public interface StatementVisitor<T, E extends Throwable> {
	default T visitVariableDeclarationAndAssignmentStatement(VariableDeclarationAndAssignmentStatement statement) throws E {
		return null;
	}

	default T visitVariableDeclarationStatement(VariableDeclarationStatement statement) throws E {
		return null;
	}

	default T visitVariableAssignmentStatement(VariableAssignmentStatement statement) throws E {
		return null;
	}

	default T visitIfStatement(IfStatement statement) throws E {
		return null;
	}

	default T visitWhileStatement(WhileStatement statement) throws E {
		return null;
	}

	default T visitDoWhileStatement(DoWhileStatement statement) throws E {
		return null;
	}

	default T visitExpressionStatement(ExpressionStatement statement) throws E {
		return null;
	}
}