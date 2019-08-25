package dev.mamo.craftlangc.ast.statement;

public interface StatementVisitor<T, U extends Throwable> {
	default T visitVariableDeclarationAndAssignmentStatement(VariableDeclarationAndAssignmentStatement statement) throws U {
		return null;
	}

	default T visitVariableDeclarationStatement(VariableDeclarationStatement statement) throws U {
		return null;
	}

	default T visitVariableAssignmentStatement(VariableAssignmentStatement statement) throws U {
		return null;
	}

	default T visitIfStatement(IfStatement statement) throws U {
		return null;
	}

	default T visitWhileStatement(WhileStatement statement) throws U {
		return null;
	}

	default T visitDoWhileStatement(DoWhileStatement statement) throws U {
		return null;
	}

	default T visitExpressionStatement(ExpressionStatement statement) throws U {
		return null;
	}
}