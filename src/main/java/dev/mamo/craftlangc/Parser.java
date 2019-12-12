package dev.mamo.craftlangc;

import dev.mamo.craftlangc.ast.*;
import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.ast.statement.*;
import dev.mamo.craftlangc.core.*;
import dev.mamo.craftlangc.core.parser.*;
import dev.mamo.craftlangc.core.parser.ParseContext.Error;

import java.math.*;
import java.util.*;
import java.util.function.*;

import static dev.mamo.craftlangc.core.parser.Parsers.*;

public class Parser {
	private static final int TAB_WIDTH = 4;
	private static final Function<ParseContext, ParseNode> UNIT;

	static {
		int[] indentValue = {0};

		Function<ParseContext, ParseNode> space = alternative(
			character(' '),
			character('\t')
		);

		Function<ParseContext, ParseNode> spaces = oneOrMore(space);
		Function<ParseContext, ParseNode> optionalSpaces = zeroOrMore(space);

		Function<ParseContext, ParseNode> indent = context -> {
			int initialPosition = context.getPosition();
			if (getIndent(parse(optionalSpaces, context).getContent()) == indentValue[0]) {
				return new ParseNode(context.getSource(), initialPosition, context.getPosition());
			} else {
				context.setPosition(initialPosition);
				context.setError("Unexpected indentation level");
				return null;
			}
		};

		Function<ParseContext, ParseNode> newline = alternative(
			string("\r\n"),
			character('\r'),
			character('\n')
		);

		Function<ParseContext, ParseNode> comment = sequence(
			character('#'),
			zeroOrMore(sequence(
				not(newline),
				any()
			))
		);

		Function<ParseContext, ParseNode> optionalComment = optional(comment);

		Function<ParseContext, ParseNode> separator = labeled(L.SEPARATOR, sequence(
			optionalSpaces,
			optionalComment,
			newline,
			zeroOrMore(sequence(
				optionalSpaces,
				optionalComment,
				newline
			))
		));

		Function<ParseContext, ParseNode> optionalSeparator = optional(separator);

		Function<Function<ParseContext, ParseNode>, Function<ParseContext, ParseNode>> indented = parser -> context -> {
			int initialPosition = context.getPosition();
			int currentIndent = indentValue[0];
			int newIndent = getIndent(parse(optionalSpaces, context).getContent());

			if (newIndent <= currentIndent) {
				context.setPosition(initialPosition);
				return null;
			}

			indentValue[0] = newIndent;

			ParseNode parsed = parse(sequence(
				parser,
				zeroOrMore(sequence(
					separator,
					indent,
					parser
				))
			), context);

			indentValue[0] = currentIndent;
			return parsed;
		};

		Function<ParseContext, ParseNode> integer = labeled(L.INTEGER, sequence(
			optional(alternative(
				character('+'),
				character('-')
			)),
			oneOrMore(range('0', '9'))
		));

		Function<ParseContext, ParseNode> name = labeled(L.NAME, oneOrMore(alternative(
			character('_'),
			range('A', 'Z'),
			range('a', 'z'),
			range('0', '9')
		)));

		Function<ParseContext, ParseNode> multiName = labeled(L.MULTI_NAME, sequence(
			name,
			zeroOrMore(sequence(
				optionalSpaces,
				character('.'),
				optionalSpaces,
				name
			))
		));

		ForwardFunction<ParseContext, ParseNode> expression = forward();

		Function<ParseContext, ParseNode> functionCall = labeled(L.FUNCTION_CALL, sequence(
			multiName,
			optionalSpaces,
			character('('),
			optionalSpaces,
			optional(sequence(
				expression,
				zeroOrMore(sequence(
					optionalSpaces,
					character(','),
					optionalSpaces,
					expression
				)),
				optionalSpaces
			)),
			character(')')
		));

		Function<ParseContext, ParseNode> command = labeled(L.COMMAND, sequence(
			character('/'),
			zeroOrMore(sequence(
				not(newline),
				any()
			))
		));

		Function<ParseContext, ParseNode> unaryExpression = labeled(L.UNARY_EXPRESSION, sequence(
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
				multiName
			)
		));

		Function<ParseContext, ParseNode> multiplicativeExpression = labeled(L.MULTIPLICATIVE_EXPRESSION, sequence(
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

		Function<ParseContext, ParseNode> additiveExpression = labeled(L.ADDITIVE_EXPRESSION, sequence(
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

		Function<ParseContext, ParseNode> andExpression = labeled(L.AND_EXPRESSION, sequence(
			additiveExpression,
			zeroOrMore(sequence(
				optionalSpaces,
				labeled(L.OPERATOR, character('&')),
				optionalSpaces,
				additiveExpression
			))
		));

		Function<ParseContext, ParseNode> xorExpression = labeled(L.XOR_EXPRESSION, sequence(
			andExpression,
			zeroOrMore(sequence(
				optionalSpaces,
				labeled(L.OPERATOR, character('^')),
				optionalSpaces,
				andExpression
			))
		));

		Function<ParseContext, ParseNode> orExpression = labeled(L.OR_EXPRESSION, sequence(
			xorExpression,
			zeroOrMore(sequence(
				optionalSpaces,
				labeled(L.OPERATOR, character('|')),
				optionalSpaces,
				xorExpression
			))
		));

		Function<ParseContext, ParseNode> comparisonExpression = labeled(L.COMPARISON_EXPRESSION, sequence(
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

		setParser(expression, comparisonExpression);

		ForwardFunction<ParseContext, ParseNode> statement = forward();

		Function<ParseContext, ParseNode> block = labeled(L.BLOCK, indented.apply(statement));

		Function<ParseContext, ParseNode> nameAndType = labeled(L.NAME_AND_TYPE, sequence(
			name,
			optionalSpaces,
			character(':'),
			optionalSpaces,
			labeled(L.TYPE, multiName)
		));

		Function<ParseContext, ParseNode> variableDeclarationAndAssignmentStatement = labeled(L.VARIABLE_DECLARATION_AND_ASSIGNMENT, sequence(
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

		Function<ParseContext, ParseNode> variableDeclarationStatement = labeled(L.VARIABLE_DECLARATION, sequence(
			string("var"),
			spaces,
			nameAndType
		));

		Function<ParseContext, ParseNode> variableAssignmentStatement = labeled(L.VARIABLE_ASSIGNMENT, sequence(
			multiName,
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

		ForwardFunction<ParseContext, ParseNode> ifStatement = forward();
		setParser(ifStatement, labeled(L.IF_STATEMENT, sequence(
			string("if"),
			spaces,
			expression,
			separator,
			labeled(L.IF_TRUE, block),
			optional(sequence(
				separator,
				indent,
				string("else"),
				alternative(
					sequence(
						spaces,
						ifStatement
					),
					sequence(
						separator,
						labeled(L.IF_FALSE, block)
					)
				)
			))
		)));

		Function<ParseContext, ParseNode> whileStatement = labeled(L.WHILE_STATEMENT, sequence(
			string("while"),
			spaces,
			expression,
			separator,
			block
		));

		Function<ParseContext, ParseNode> doWhileStatement = labeled(L.DO_WHILE_STATEMENT, sequence(
			string("do"),
			separator,
			block,
			separator,
			indent,
			string("while"),
			spaces,
			expression
		));

		setParser(statement, alternative(
			variableDeclarationAndAssignmentStatement,
			variableDeclarationStatement,
			variableAssignmentStatement,
			ifStatement,
			whileStatement,
			doWhileStatement,
			expression
		));

		Function<ParseContext, ParseNode> typeDeclaration = labeled(L.TYPE_DECLARATION, sequence(
			string("type"),
			spaces,
			name,
			separator,
			indented.apply(nameAndType)
		));

		Function<ParseContext, ParseNode> functionDefinition = labeled(L.FUNCTION_DEFINITION, sequence(
			zeroOrMore(
				sequence(string("tag"),
					spaces,
					labeled(L.TAG, multiName),
					separator
				)),
			string("fun"),
			spaces,
			name,
			optionalSpaces,
			character('('),
			optionalSpaces,
			optional(sequence(
				nameAndType,
				zeroOrMore(sequence(
					optionalSpaces,
					character(','),
					optionalSpaces,
					nameAndType
				)),
				optionalSpaces
			)),
			character(')'),
			optional(sequence(
				optionalSpaces,
				character(':'),
				optionalSpaces,
				labeled(L.RETURN_TYPE, multiName)
			)),
			separator,
			block
		));

		Function<ParseContext, ParseNode> part = alternative(
			typeDeclaration,
			variableDeclarationStatement,
			functionDefinition
		);

		UNIT = labeled(L.UNIT, sequence(
			optionalSeparator,
			optional(sequence(
				sequence(
					string("namespace"),
					spaces,
					multiName
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

	private Parser() {}

	public static Unit parse(ParseContext context) {
		ParseNode parsed = parse(UNIT, context);
		if (parsed != null) {
			parsed.flatten();
			return parseUnit(parsed);
		} else {
			Error error = context.getFurthestError();
			throw new ParseException(error.getPosition(), error.getMessage());
		}
	}

	private static ParseNode parse(Function<ParseContext, ParseNode> parser, ParseContext context) {
		return Parsers.parse(parser, context);
	}

	private static Unit parseUnit(ParseNode unit) {
		Namespace namespace = null;
		List<TypeDeclaration> typeDeclarations = new ArrayList<>();
		List<VariableDeclarationStatement> variableDeclarations = new ArrayList<>();
		List<FunctionDefinition> functions = new ArrayList<>();

		for (ParseNode child : unit.getChildren()) {
			switch (child.getLabel()) {
				case L.MULTI_NAME:
					namespace = new Namespace(parseMultiName(child));
					break;
				case L.TYPE_DECLARATION:
					typeDeclarations.add(parseTypeDeclaration(child));
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

		return new Unit(unit, namespace, typeDeclarations, variableDeclarations, functions);
	}

	private static TypeDeclaration parseTypeDeclaration(ParseNode typeDefinition) {
		String name = null;
		List<TypeAndName> members = new ArrayList<>();

		for (ParseNode child : typeDefinition.getChildren()) {
			switch (child.getLabel()) {
				case L.NAME:
					name = child.getContent();
					break;
				case L.NAME_AND_TYPE:
					members.add(parseNameAndType(child));
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new TypeDeclaration(typeDefinition, name, members);
	}

	private static FunctionDefinition parseFunctionDefinition(ParseNode functionDefinition) {
		List<FQN> tags = new ArrayList<>();
		FQN returnTypeFQN = null;
		String name = null;
		List<TypeAndName> parameters = new ArrayList<>();
		List<Statement> body = null;

		for (ParseNode child : functionDefinition.getChildren()) {
			switch (child.getLabel()) {
				case L.TAG:
					tags.add(new FQN(parseMultiName(child)));
					break;
				case L.RETURN_TYPE:
					returnTypeFQN = new FQN(parseMultiName(child));
					break;
				case L.NAME:
					name = child.getContent();
					break;
				case L.NAME_AND_TYPE:
					parameters.add(parseNameAndType(child));
					break;
				case L.BLOCK:
					body = parseBlock(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		assert body != null;
		return new FunctionDefinition(functionDefinition, tags, returnTypeFQN, name, parameters, body);
	}

	private static TypeAndName parseNameAndType(ParseNode parameter) {
		FQN typeFQN = null;
		String name = null;

		for (ParseNode child : parameter.getChildren()) {
			switch (child.getLabel()) {
				case L.TYPE:
					typeFQN = new FQN(parseMultiName(child));
					break;
				case L.NAME:
					name = child.getContent();
					break;
				default:
					assert false : child.getLabel();
			}
		}

		return new TypeAndName(parameter, typeFQN, name);
	}

	private static List<Statement> parseBlock(ParseNode block) {
		List<Statement> statements = new ArrayList<>();

		for (ParseNode child : block.getChildren()) {
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
					statements.add(new ExpressionStatement(block, parseComparisonExpression(child)));
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return statements;
	}

	private static VariableDeclarationAndAssignmentStatement parseVariableDeclarationAndAssignment(ParseNode statement) {
		FQN variableTypeFQN = null;
		String variableName = null;
		Expression assignedValue = null;

		for (ParseNode child : statement.getChildren()) {
			switch (child.getLabel()) {
				case L.TYPE:
					variableTypeFQN = new FQN(parseMultiName(child));
					break;
				case L.NAME:
					variableName = child.getContent();
					break;
				case L.COMPARISON_EXPRESSION:
					assignedValue = parseComparisonExpression(child);
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new VariableDeclarationAndAssignmentStatement(statement, variableTypeFQN, variableName, assignedValue);
	}

	private static VariableDeclarationStatement parseVariableDeclaration(ParseNode statement) {
		List<ParseNode> children = statement.getChildren();
		if (children.size() == 1) {
			ParseNode child = children.get(0);
			if (child.getLabel().equals(L.NAME_AND_TYPE)) {
				TypeAndName typeAndName = parseNameAndType(children.get(0));
				return new VariableDeclarationStatement(statement, typeAndName.getTypeFQN(), typeAndName.getName());
			}
		}
		assert false;
		return null;
	}

	private static VariableAssignmentStatement parseVariableAssignment(ParseNode statement) {
		FQN variableFQN = null;
		AssignmentOperator operator = null;
		Expression value = null;

		for (ParseNode child : statement.getChildren()) {
			switch (child.getLabel()) {
				case L.MULTI_NAME:
					variableFQN = new FQN(parseMultiName(child));
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

		return new VariableAssignmentStatement(statement, variableFQN, operator, value);
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
					ifTrue = parseBlock(child);
					break;
				case L.IF_FALSE:
					ifFalse = parseBlock(child);
					break;
				case L.IF_STATEMENT:
					ifFalse = new ArrayList<>();
					ifFalse.add(parseIfStatement(child));
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
				case L.BLOCK:
					body = parseBlock(child);
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
				case L.BLOCK:
					body = parseBlock(child);
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
					result = result == null
						? parseMultiplicativeExpression(child)
						: new BinaryExpression(child, result, operator, parseMultiplicativeExpression(child));
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
					result = result == null ? parseUnaryExpression(child) : new BinaryExpression(child, result, operator, parseUnaryExpression(child));
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
				case L.MULTI_NAME:
					result = new VariableExpression(child, new FQN(parseMultiName(child)));
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

	private static CallExpression parseFunctionCall(ParseNode functionCall) {
		FQN functionFQN = null;
		List<Expression> arguments = new ArrayList<>();

		for (ParseNode child : functionCall.getChildren()) {
			switch (child.getLabel()) {
				case L.MULTI_NAME:
					functionFQN = new FQN(parseMultiName(child));
					break;
				case L.COMPARISON_EXPRESSION:
					arguments.add(parseComparisonExpression(child));
					break;
				default:
					assert false : child.getLabel();
					break;
			}
		}

		return new CallExpression(functionCall, functionFQN, arguments);
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

	private static List<String> parseMultiName(ParseNode multiName) {
		List<String> result = new ArrayList<>();

		for (ParseNode child : multiName.getChildren()) {
			if (L.NAME.equals(child.getLabel())) {
				result.add(child.getContent());
			} else {
				assert false : child.getLabel();
			}
		}

		return result;
	}

	private static int getIndent(String spaces) {
		int indent = 0;
		for (int i = 0, spacesLength = spaces.length(); i < spacesLength; i++) {
			if (spaces.charAt(i) == '\t') {
				indent = (indent + TAB_WIDTH) / TAB_WIDTH * TAB_WIDTH;
			} else {
				indent++;
			}
		}
		return indent;
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
		public static final String SEPARATOR = "separator";
		public static final String INTEGER = "integer";
		public static final String NAME = "name";
		public static final String MULTI_NAME = "multi-name";
		public static final String FUNCTION_CALL = "function call";
		public static final String COMMAND = "command";
		public static final String OPERATOR = "operator";
		public static final String UNARY_EXPRESSION = "unary expression";
		public static final String MULTIPLICATIVE_EXPRESSION = "multiplicative expression";
		public static final String ADDITIVE_EXPRESSION = "additive expression";
		public static final String AND_EXPRESSION = "and expression";
		public static final String XOR_EXPRESSION = "xor expression";
		public static final String OR_EXPRESSION = "or expression";
		public static final String COMPARISON_EXPRESSION = "comparison expression";
		public static final String BLOCK = "block";
		public static final String NAME_AND_TYPE = "name and type";
		public static final String TYPE = "type";
		public static final String VARIABLE_DECLARATION_AND_ASSIGNMENT = "variable declaration and assignment";
		public static final String VARIABLE_DECLARATION = "variable declaration";
		public static final String VARIABLE_ASSIGNMENT = "variable assignment";
		public static final String IF_STATEMENT = "if statement";
		public static final String IF_TRUE = "if true";
		public static final String IF_FALSE = "if false";
		public static final String WHILE_STATEMENT = "while statement";
		public static final String DO_WHILE_STATEMENT = "do-while statement";
		public static final String TYPE_DECLARATION = "type declaration";
		public static final String FUNCTION_DEFINITION = "function definition";
		public static final String TAG = "tag";
		public static final String RETURN_TYPE = "return type";
		public static final String UNIT = "unit";

		private L() {}
	}
}