package dev.mamo.craftlangc;

import dev.mamo.craftlangc.ast.*;
import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.ast.statement.*;
import dev.mamo.craftlangc.core.parser.*;
import dev.mamo.craftlangc.core.parser.ParseContext.Error;

import java.math.*;
import java.util.*;

import static dev.mamo.craftlangc.core.parser.Parsers.*;

public class CraftlangParser {
	private static final int TAB_WIDTH = 4;
	private static final Parser UNIT;

	static {
		Parser space = alternative(
			character(' '),
			character('\t')
		);

		Parser spaces = oneOrMore(space);
		Parser optionalSpaces = zeroOrMore(space);

		Parser indent = context -> {
			int initialPosition = context.getPosition();
			if (getIndentValue(optionalSpaces.parse(context).getContent()) == (int) context.get("indentValue", 0)) {
				return new ParseNode(context.getSource(), initialPosition, context.getPosition());
			} else {
				context.setPosition(initialPosition);
				context.setError("Unexpected indentation level");
				return null;
			}
		};

		Parser newline = alternative(
			string("\r\n"),
			character('\r'),
			character('\n')
		);

		Parser comment = sequence(
			character('#'),
			zeroOrMore(sequence(
				not(newline),
				any()
			))
		);

		Parser optionalComment = optional(comment);

		Parser separator = sequence(
			optionalSpaces,
			optionalComment,
			newline,
			zeroOrMore(sequence(
				optionalSpaces,
				optionalComment,
				newline
			))
		);

		Parser optionalSeparator = optional(separator);

		Parser integer = labeled(L.INTEGER, sequence(
			optional(alternative(
				character('+'),
				character('-')
			)),
			oneOrMore(range('0', '9'))
		));

		Parser name = labeled(L.NAME, oneOrMore(alternative(
			character('_'),
			range('A', 'Z'),
			range('a', 'z'),
			range('0', '9')
		)));

		Parser namespace = labeled(L.NAMESPACE, sequence(
			name,
			zeroOrMore(sequence(
				optionalSpaces,
				character('.'),
				optionalSpaces,
				name
			))
		));

		Parser qualifiedName = labeled(L.QUALIFIED_NAME, sequence(
			optional(sequence(
				namespace,
				optionalSpaces,
				character('.'),
				optionalSpaces
			)),
			name
		));

		ForwardParser expression = forward();

		Parser argument = alternative(
			sequence(
				string("ref"),
				spaces,
				labeled(L.REFERENCE, qualifiedName)
			),
			expression
		);

		Parser functionCall = labeled(L.FUNCTION_CALL, sequence(
			qualifiedName,
			optionalSpaces,
			character('('),
			optionalSpaces,
			optional(sequence(
				argument,
				zeroOrMore(sequence(
					optionalSpaces,
					character(','),
					optionalSpaces,
					argument
				)),
				optionalSpaces
			)),
			character(')')
		));

		Parser command = labeled(L.COMMAND, sequence(
			character('/'),
			zeroOrMore(sequence(
				not(newline),
				any()
			))
		));

		Parser unaryExpression = labeled(L.UNARY_EXPRESSION, sequence(
			zeroOrMore(sequence(
				not(integer),
				labeled(L.OPERATOR, alternative(
					character('+'),
					character('-'),
					character('!')
				))
			)),
			alternative(
				sequence(
					character('('),
					optionalSpaces,
					expression,
					optionalSpaces,
					character(')')
				),
				functionCall,
				command,
				integer,
				qualifiedName
			)
		));

		Parser multiplicativeExpression = labeled(L.MULTIPLICATIVE_EXPRESSION, sequence(
			unaryExpression,
			zeroOrMore(sequence(
				optionalSpaces,
				labeled(L.OPERATOR, alternative(
					character('*'),
					character('/'),
					character('%')
				)),
				optionalSpaces,
				unaryExpression
			))
		));

		Parser additiveExpression = labeled(L.ADDITIVE_EXPRESSION, sequence(
			multiplicativeExpression,
			zeroOrMore(sequence(
				optionalSpaces,
				labeled(L.OPERATOR, alternative(
					character('+'),
					character('-')
				)),
				optionalSpaces,
				multiplicativeExpression
			))
		));

		Parser andExpression = labeled(L.AND_EXPRESSION, sequence(
			additiveExpression,
			zeroOrMore(sequence(
				optionalSpaces,
				labeled(L.OPERATOR, character('&')),
				optionalSpaces,
				additiveExpression
			))
		));

		Parser xorExpression = labeled(L.XOR_EXPRESSION, sequence(
			andExpression,
			zeroOrMore(sequence(
				optionalSpaces,
				labeled(L.OPERATOR, character('^')),
				optionalSpaces,
				andExpression
			))
		));

		Parser orExpression = labeled(L.OR_EXPRESSION, sequence(
			xorExpression,
			zeroOrMore(sequence(
				optionalSpaces,
				labeled(L.OPERATOR, character('|')),
				optionalSpaces,
				xorExpression
			))
		));

		Parser comparisonExpression = labeled(L.COMPARISON_EXPRESSION, sequence(
			orExpression,
			zeroOrMore(sequence(
				optionalSpaces,
				labeled(L.OPERATOR, alternative(
					string("=="),
					string("!="),
					string("<="),
					character('<'),
					string(">="),
					character('>')
				)),
				optionalSpaces,
				orExpression
			))
		));

		expression.setParser(comparisonExpression);

		ForwardParser statement = forward();

		Parser body = labeled(L.BODY, context -> {
			int initialPosition = context.getPosition();
			int currentIndent = (int) context.get("indentValue", 0);
			int newIndent = getIndentValue(optionalSpaces.parse(context).getContent());

			if (newIndent <= currentIndent) {
				context.setPosition(initialPosition);
				return null;
			}

			context.set("indentValue", newIndent);

			ParseNode parsed = sequence(
				statement,
				zeroOrMore(sequence(
					separator,
					indent,
					statement
				))
			).parse(context);

			context.set("indentValue", currentIndent);
			return parsed;
		});

		Parser variableDeclarationAndAssignmentStatement = labeled(L.VARIABLE_DECLARATION_AND_ASSIGNMENT, sequence(
			string("var"),
			spaces,
			name,
			optional(sequence(
				optionalSpaces,
				character(':'),
				optionalSpaces,
				labeled(L.TYPE, name)
			)),
			optionalSpaces,
			character('='),
			optionalSpaces,
			expression
		));

		Parser variableDeclarationStatement = labeled(L.VARIABLE_DECLARATION, sequence(
			string("var"),
			spaces,
			name,
			optionalSpaces,
			character(':'),
			optionalSpaces,
			labeled(L.TYPE, name)
		));

		Parser variableAssignmentStatement = labeled(L.VARIABLE_ASSIGNMENT, sequence(
			qualifiedName,
			optionalSpaces,
			labeled(L.OPERATOR, alternative(
				character('='),
				string("+="),
				string("-="),
				string("*="),
				string("/="),
				string("%="),
				string("&="),
				string("^="),
				string("|=")
			)),
			optionalSpaces,
			expression
		));

		Parser ifStatement = labeled(L.IF_STATEMENT, sequence(
			string("if"),
			spaces,
			expression,
			separator,
			labeled(L.IF_TRUE, body),
			optional(sequence(
				separator,
				indent,
				string("else"),
				separator,
				labeled(L.IF_FALSE, body)
			))
		));

		Parser whileStatement = labeled(L.WHILE_STATEMENT, sequence(
			string("while"),
			spaces,
			expression,
			separator,
			body
		));

		Parser doWhileStatement = labeled(L.DO_WHILE_STATEMENT, sequence(
			string("do"),
			separator,
			body,
			indent,
			string("while"),
			spaces,
			expression
		));

		statement.setParser(alternative(
			variableDeclarationAndAssignmentStatement,
			variableDeclarationStatement,
			variableAssignmentStatement,
			ifStatement,
			whileStatement,
			doWhileStatement,
			expression
		));

		Parser parameter = labeled(L.PARAMETER, sequence(
			name,
			optionalSpaces,
			character(':'),
			optionalSpaces,
			labeled(L.TYPE, name)
		));

		Parser functionDefinition = labeled(L.FUNCTION_DEFINITION, sequence(
			zeroOrMore(
				sequence(string("tag"),
					spaces,
					labeled(L.TAG, qualifiedName),
					separator
				)),
			string("fun"),
			spaces,
			name,
			optionalSpaces,
			character('('),
			optionalSpaces,
			optional(sequence(
				parameter,
				zeroOrMore(sequence(
					optionalSpaces,
					character(','),
					optionalSpaces,
					parameter
				)),
				optionalSpaces
			)),
			character(')'),
			optional(sequence(
				optionalSpaces,
				character(':'),
				optionalSpaces,
				labeled(L.RETURN_TYPE, name)
			)),
			separator,
			body
		));

		Parser part = alternative(
			variableDeclarationStatement,
			functionDefinition
		);

		UNIT = labeled(L.UNIT, sequence(
			optionalSeparator,
			optional(sequence(
				alternative(
					sequence(
						string("namespace"),
						spaces,
						namespace
					),
					part
				),
				zeroOrMore(sequence(
					separator,
					part
				)),
				optionalSeparator
			)),
			optionalSpaces,
			optionalComment,
			end()
		));
	}

	private CraftlangParser() {}

	public static Unit parse(ParseContext context) {
		ParseNode parsed = UNIT.parse(context);
		if (parsed != null) {
			return parseUnit(parsed.flatten());
		} else {
			Error error = context.getFurthestError();
			throw new ParseException(error.getPosition(), error.getMessage());
		}
	}

	private static Unit parseUnit(ParseNode unit) {
		Namespace namespace = null;
		List<VariableDeclarationStatement> variableDeclarations = new ArrayList<>();
		List<FunctionDefinition> functions = new ArrayList<>();

		for (ParseNode child : unit.getChildren()) {
			switch (child.getLabel()) {
				case L.NAMESPACE:
					namespace = parseNamespace(child);
					break;
				case L.VARIABLE_DECLARATION:
					variableDeclarations.add(parseVariableDeclaration(child));
					break;
				case L.FUNCTION_DEFINITION:
					functions.add(parseFunctionDefinition(child));
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new Unit(unit, namespace, variableDeclarations, functions);
	}

	private static FunctionDefinition parseFunctionDefinition(ParseNode functionDefinition) {
		List<QualifiedName> tags = new ArrayList<>();
		Type returnType = null;
		String name = null;
		List<Parameter> parameters = new ArrayList<>();
		List<Statement> body = null;

		for (ParseNode child : functionDefinition.getChildren()) {
			switch (child.getLabel()) {
				case L.TAG:
					QualifiedName tag = parseQualifiedName(child);
					if (tag.getNamespace() == null) {
						tag = new QualifiedName(new Namespace("minecraft"), tag.getName());
					}
					tags.add(tag);
					break;
				case L.RETURN_TYPE:
					returnType = parseType(child);
					break;
				case L.NAME:
					name = child.getContent();
					break;
				case L.PARAMETER:
					parameters.add(parseParameter(child));
					break;
				case L.BODY:
					body = parseBody(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		assert body != null;
		return new FunctionDefinition(functionDefinition, tags, returnType, name, parameters, body);
	}

	private static Parameter parseParameter(ParseNode parameter) {
		Type type = null;
		String name = null;

		for (ParseNode child : parameter.getChildren()) {
			switch (child.getLabel()) {
				case L.TYPE:
					type = parseType(child);
					break;
				case L.NAME:
					name = child.getContent();
					break;
				default:
					assert false : child.getLabel();
			}
		}

		return new Parameter(parameter, type, name);
	}

	private static List<Statement> parseBody(ParseNode body) {
		List<Statement> statements = new ArrayList<>();

		for (ParseNode child : body.getChildren()) {
			switch (child.getLabel()) {
				case L.VARIABLE_DECLARATION_AND_ASSIGNMENT:
					statements.add(parseVariableDeclarationAndAssignment(child));
					break;
				case L.VARIABLE_DECLARATION:
					statements.add(parseVariableDeclaration(child));
					break;
				case L.VARIABLE_ASSIGNMENT:
					statements.add(parseVariableAssignment(child));
					break;
				case L.IF_STATEMENT:
					statements.add(parseIfStatement(child));
					break;
				case L.WHILE_STATEMENT:
					statements.add(parseWhileStatement(child));
					break;
				case L.DO_WHILE_STATEMENT:
					statements.add(parseDoWhileStatement(child));
					break;
				case L.COMPARISON_EXPRESSION:
					statements.add(new ExpressionStatement(body, parseComparisonExpression(child)));
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return statements;
	}

	private static VariableDeclarationAndAssignmentStatement parseVariableDeclarationAndAssignment(ParseNode statement) {
		Type type = null;
		String name = null;
		Expression value = null;

		for (ParseNode child : statement.getChildren()) {
			switch (child.getLabel()) {
				case L.TYPE:
					type = parseType(child);
					break;
				case L.NAME:
					name = child.getContent();
					break;
				case L.COMPARISON_EXPRESSION:
					value = parseComparisonExpression(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new VariableDeclarationAndAssignmentStatement(statement, type, name, value);
	}

	private static VariableDeclarationStatement parseVariableDeclaration(ParseNode statement) {
		Type type = null;
		String name = null;

		for (ParseNode child : statement.getChildren()) {
			switch (child.getLabel()) {
				case L.TYPE:
					type = parseType(child);
					break;
				case L.NAME:
					name = child.getContent();
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new VariableDeclarationStatement(statement, type, name);
	}

	private static VariableAssignmentStatement parseVariableAssignment(ParseNode statement) {
		QualifiedName name = null;
		AssignmentOperator operator = null;
		Expression value = null;

		for (ParseNode child : statement.getChildren()) {
			switch (child.getLabel()) {
				case L.QUALIFIED_NAME:
					name = parseQualifiedName(child);
					break;
				case L.OPERATOR:
					operator = parseAssignmentOperator(child);
					break;
				case L.COMPARISON_EXPRESSION:
					value = parseComparisonExpression(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new VariableAssignmentStatement(statement, name, operator, value);
	}

	private static IfStatement parseIfStatement(ParseNode statement) {
		Expression condition = null;
		List<Statement> ifTrue = null;
		List<Statement> ifFalse = null;

		for (ParseNode child : statement.getChildren()) {
			switch (child.getLabel()) {
				case L.COMPARISON_EXPRESSION:
					condition = parseComparisonExpression(child);
					break;
				case L.IF_TRUE:
					ifTrue = parseBody(child);
					break;
				case L.IF_FALSE:
					ifFalse = parseBody(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		assert ifTrue != null;
		return new IfStatement(statement, condition, ifTrue, ifFalse != null ? ifFalse : new ArrayList<>());
	}

	private static WhileStatement parseWhileStatement(ParseNode statement) {
		Expression condition = null;
		List<Statement> body = null;

		for (ParseNode child : statement.getChildren()) {
			switch (child.getLabel()) {
				case L.COMPARISON_EXPRESSION:
					condition = parseComparisonExpression(child);
					break;
				case L.BODY:
					body = parseBody(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		assert body != null;
		return new WhileStatement(statement, condition, body);
	}

	private static DoWhileStatement parseDoWhileStatement(ParseNode statement) {
		List<Statement> body = null;
		Expression condition = null;

		for (ParseNode child : statement.getChildren()) {
			switch (child.getLabel()) {
				case L.BODY:
					body = parseBody(child);
					break;
				case L.COMPARISON_EXPRESSION:
					condition = parseComparisonExpression(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		assert body != null;
		return new DoWhileStatement(statement, body, condition);
	}

	private static Type parseType(ParseNode type) {
		switch (type.getContent()) {
			case "bool":
				return Type.BOOLEAN;
			case "int":
				return Type.INTEGER;
			case "void":
				return null;
			default:
				throw new ParseException(type.getBeginIndex(), "Illegal type: " + type.getContent());
		}
	}

	private static AssignmentOperator parseAssignmentOperator(ParseNode operator) {
		switch (operator.getContent()) {
			case "=":
				return AssignmentOperator.EQUAL;
			case "+=":
				return AssignmentOperator.PLUS_EQUAL;
			case "-=":
				return AssignmentOperator.MINUS_EQUAL;
			case "*=":
				return AssignmentOperator.TIMES_EQUAL;
			case "/=":
				return AssignmentOperator.DIVIDE_EQUAL;
			case "%=":
				return AssignmentOperator.REMAINDER_EQUAL;
			case "&=":
				return AssignmentOperator.AND_EQUAL;
			case "^=":
				return AssignmentOperator.XOR_EQUAL;
			case "|=":
				return AssignmentOperator.OR_EQUAL;
			default:
				assert false : operator.getContent();
				return null;
		}
	}

	private static Expression parseComparisonExpression(ParseNode expression) {
		Expression result = null;
		BinaryOperator operator = null;

		for (ParseNode child : expression.getChildren()) {
			switch (child.getLabel()) {
				case L.OR_EXPRESSION:
					result = result == null
						? parseOrExpression(child)
						: new BinaryExpression(child, result, operator, parseOrExpression(child));
					break;
				case L.OPERATOR:
					operator = parseBinaryOperator(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return result;
	}

	private static Expression parseOrExpression(ParseNode expression) {
		Expression result = null;
		BinaryOperator operator = null;

		for (ParseNode child : expression.getChildren()) {
			switch (child.getLabel()) {
				case L.XOR_EXPRESSION:
					result = result == null
						? parseXorExpression(child)
						: new BinaryExpression(child, result, operator, parseXorExpression(child));
					break;
				case L.OPERATOR:
					operator = parseBinaryOperator(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return result;
	}

	private static Expression parseXorExpression(ParseNode expression) {
		Expression result = null;
		BinaryOperator operator = null;

		for (ParseNode child : expression.getChildren()) {
			switch (child.getLabel()) {
				case L.AND_EXPRESSION:
					result = result == null
						? parseAndExpression(child)
						: new BinaryExpression(child, result, operator, parseAndExpression(child));
					break;
				case L.OPERATOR:
					operator = parseBinaryOperator(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return result;
	}

	private static Expression parseAndExpression(ParseNode expression) {
		Expression result = null;
		BinaryOperator operator = null;

		for (ParseNode child : expression.getChildren()) {
			switch (child.getLabel()) {
				case L.ADDITIVE_EXPRESSION:
					result = result == null
						? parseAdditiveExpression(child)
						: new BinaryExpression(child, result, operator, parseAdditiveExpression(child));
					break;
				case L.OPERATOR:
					operator = parseBinaryOperator(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return result;
	}

	private static Expression parseAdditiveExpression(ParseNode expression) {
		Expression result = null;
		BinaryOperator operator = null;

		for (ParseNode child : expression.getChildren()) {
			switch (child.getLabel()) {
				case L.MULTIPLICATIVE_EXPRESSION:
					result = result == null ? parseMultiplicativeExpression(child) : new BinaryExpression(child, result, operator, parseMultiplicativeExpression(child));
					break;
				case L.OPERATOR:
					operator = parseBinaryOperator(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return result;
	}

	private static Expression parseMultiplicativeExpression(ParseNode expression) {
		Expression result = null;
		BinaryOperator operator = null;

		for (ParseNode child : expression.getChildren()) {
			switch (child.getLabel()) {
				case L.UNARY_EXPRESSION:
					result = result == null
						? parseUnaryExpression(child)
						: new BinaryExpression(child, result, operator, parseUnaryExpression(child));
					break;
				case L.OPERATOR:
					operator = parseBinaryOperator(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return result;
	}

	private static Expression parseUnaryExpression(ParseNode expression) {
		List<UnaryOperator> operators = new ArrayList<>();
		Expression result = null;

		for (ParseNode child : expression.getChildren()) {
			switch (child.getLabel()) {
				case L.OPERATOR:
					operators.add(parseUnaryOperator(child));
					break;
				case L.COMPARISON_EXPRESSION:
					result = parseComparisonExpression(child);
					break;
				case L.FUNCTION_CALL:
					result = parseFunctionCall(child);
					break;
				case L.COMMAND:
					result = new CommandExpression(child, child.getSource().substring(child.getBeginIndex() + 1, child.getEndIndex()));
					break;
				case L.INTEGER:
					result = new IntegerExpression(child, new BigInteger(child.getContent()).intValue());
					break;
				case L.QUALIFIED_NAME:
					result = new VariableExpression(child, parseQualifiedName(child));
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		for (UnaryOperator operator : operators) {
			result = new UnaryExpression(expression, operator, result);
		}

		return result;
	}

	private static FunctionCallExpression parseFunctionCall(ParseNode functionCall) {
		QualifiedName name = null;
		List<Argument> arguments = new ArrayList<>();

		for (ParseNode child : functionCall.getChildren()) {
			switch (child.getLabel()) {
				case L.QUALIFIED_NAME:
					name = parseQualifiedName(child);
					break;
				case L.REFERENCE:
					arguments.add(new Argument(child, true, new VariableExpression(child, parseQualifiedName(child))));
					break;
				case L.COMPARISON_EXPRESSION:
					arguments.add(new Argument(child, false, parseComparisonExpression(child)));
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new FunctionCallExpression(functionCall, name, arguments);
	}

	private static BinaryOperator parseBinaryOperator(ParseNode operator) {
		switch (operator.getContent()) {
			case "+":
				return BinaryOperator.PLUS;
			case "-":
				return BinaryOperator.MINUS;
			case "*":
				return BinaryOperator.TIMES;
			case "/":
				return BinaryOperator.DIVIDE;
			case "%":
				return BinaryOperator.REMAINDER;
			case "&":
				return BinaryOperator.AND;
			case "|":
				return BinaryOperator.OR;
			case "^":
				return BinaryOperator.XOR;
			case "==":
				return BinaryOperator.EQUAL;
			case "!=":
				return BinaryOperator.NOT_EQUAL;
			case "<=":
				return BinaryOperator.LESS_OR_EQUAL;
			case "<":
				return BinaryOperator.LESS;
			case ">=":
				return BinaryOperator.GREATER_OR_EQUAL;
			case ">":
				return BinaryOperator.GREATER;
			default:
				assert false : operator.getContent();
				return null;
		}
	}

	private static UnaryOperator parseUnaryOperator(ParseNode operator) {
		switch (operator.getContent()) {
			case "!":
				return UnaryOperator.NOT;
			case "+":
				return UnaryOperator.PLUS;
			case "-":
				return UnaryOperator.MINUS;
			default:
				assert false : operator.getContent();
				return null;
		}
	}

	private static QualifiedName parseQualifiedName(ParseNode qualifiedName) {
		Namespace namespace = null;
		String name = null;

		for (ParseNode child : qualifiedName.getChildren()) {
			switch (child.getLabel()) {
				case L.NAMESPACE:
					namespace = parseNamespace(child);
					break;
				case L.NAME:
					name = child.getContent();
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new QualifiedName(namespace, name);
	}

	private static Namespace parseNamespace(ParseNode namespace) {
		List<String> components = new ArrayList<>();

		for (ParseNode child : namespace.getChildren()) {
			switch (child.getLabel()) {
				case L.NAME:
					components.add(child.getContent());
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new Namespace(components);
	}

	private static int getIndentValue(String spaces) {
		int indentValue = 0;
		for (int i = 0, spacesLength = spaces.length(); i < spacesLength; i++) {
			if (spaces.charAt(i) == '\t') {
				indentValue = (indentValue + TAB_WIDTH) / TAB_WIDTH * TAB_WIDTH;
			} else {
				indentValue++;
			}
		}
		return indentValue;
	}

	public static class ParseException extends RuntimeException {
		private final int position;

		public ParseException(int position, String message) {
			super(message);
			this.position = position;
		}

		public int getPosition() {
			return position;
		}
	}

	private static class L {
		public static final String ADDITIVE_EXPRESSION = "additiveExpression";
		public static final String AND_EXPRESSION = "andExpression";
		public static final String BODY = "body";
		public static final String COMMAND = "command";
		public static final String COMPARISON_EXPRESSION = "comparisonExpression";
		public static final String DO_WHILE_STATEMENT = "doWhileStatement";
		public static final String FUNCTION_CALL = "functionCall";
		public static final String FUNCTION_DEFINITION = "functionDefinition";
		public static final String IF_FALSE = "ifFalse";
		public static final String IF_STATEMENT = "ifStatement";
		public static final String IF_TRUE = "ifTrue";
		public static final String INTEGER = "integer";
		public static final String MULTIPLICATIVE_EXPRESSION = "multiplicativeExpression";
		public static final String NAME = "name";
		public static final String NAMESPACE = "namespace";
		public static final String OPERATOR = "operator";
		public static final String OR_EXPRESSION = "orExpression";
		public static final String PARAMETER = "parameter";
		public static final String QUALIFIED_NAME = "qualifiedName";
		public static final String REFERENCE = "reference";
		public static final String RETURN_TYPE = "returnType";
		public static final String TAG = "tag";
		public static final String TYPE = "type";
		public static final String UNARY_EXPRESSION = "unaryExpression";
		public static final String UNIT = "unit";
		public static final String VARIABLE_ASSIGNMENT = "variableAssignment";
		public static final String VARIABLE_DECLARATION = "variableDeclaration";
		public static final String VARIABLE_DECLARATION_AND_ASSIGNMENT = "variableDeclarationAndAssignment";
		public static final String WHILE_STATEMENT = "whileStatement";
		public static final String XOR_EXPRESSION = "xorExpression";

		private L() {}
	}
}