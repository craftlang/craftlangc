package dev.mamo.craftlangc;

import dev.mamo.craftlangc.ast.*;
import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.ast.statement.*;
import dev.mamo.craftlangc.core.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class CraftlangCompiler {
	private static final String NL = "\r\n";
	private static final String TAB = "\t";

	private CraftlangCompiler() {}

	public static void compile(Path base, List<Unit> units) throws IOException {
		writeln(
			base.resolve("pack.mcmeta"),
			"{" + NL
				+ TAB + "\"pack\": {" + NL
				+ TAB + TAB + "\"pack_format\": 4," + NL
				+ TAB + TAB + "\"description\": \"\"" + NL
				+ TAB + "}" + NL
				+ "}"
		);

		if (units.isEmpty()) {
			return;
		}

		Map<QualifiedName, Variable> globals = new HashMap<>();
		Map<QualifiedName, FunctionDefinition> functions = new HashMap<>();
		Map<QualifiedName, Set<QualifiedName>> tags = new HashMap<>();
		Map<Namespace, Integer> maxParameterCounts = new HashMap<>();
		Map<Namespace, Integer> maxLocalCounts = new HashMap<>();
		Map<Namespace, Integer> maxStackCounts = new HashMap<>();

		// Associate functions to their fully qualified names and tags, keep track of the globally defined variables and
		// the maximum number of parameters function use, for each unit
		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();

			QualifiedName loadName = new QualifiedName(new Namespace("minecraft"), "load");
			tags.putIfAbsent(loadName, new LinkedHashSet<>());
			tags.get(loadName).add(new QualifiedName(getCraftlangNamespace(namespace), "load"));

			for (VariableDeclarationStatement variableDeclaration : unit.getVariableDeclarations()) {
				QualifiedName variableName = new QualifiedName(namespace, variableDeclaration.getName());

				if (globals.containsKey(variableName)) {
					throw new CompileException(variableDeclaration.getSource().getBeginIndex(), "Variable already defined: " + variableName);
				}

				globals.put(variableName, new Variable(variableDeclaration.getType(), "cr_" + Util.toBase62(namespace.hashCode()) + "_" + (globals.size() + 1)));
			}

			for (FunctionDefinition function : unit.getFunctions()) {
				QualifiedName functionName = new QualifiedName(namespace, function.getName());

				if (functions.containsKey(functionName)) {
					throw new CompileException(function.getSource().getBeginIndex(), "Function already defined: " + functionName);
				}

				int parameterCount = function.getParameters().size();
				List<QualifiedName> functionTags = function.getTags();

				functions.put(functionName, function);
				maxParameterCounts.put(namespace, Math.max(maxParameterCounts.getOrDefault(namespace, 0), parameterCount));

				if (!functionTags.isEmpty()) {
					if (parameterCount > 0) {
						throw new CompileException(function.getSource().getBeginIndex(), "Can't tag a function with a non-empty parameter list");
					}

					for (QualifiedName tag : functionTags) {
						tags.putIfAbsent(tag, new LinkedHashSet<>());
						tags.get(tag).add(functionName);
					}
				}
			}
		}

		// Generate the JSON tag files for each function
		for (Map.Entry<QualifiedName, Set<QualifiedName>> entry : tags.entrySet()) {
			Iterator<QualifiedName> functionNamesIterator = entry.getValue().iterator();
			if (!functionNamesIterator.hasNext()) {
				continue;
			}

			Path tagPath = getTagPath(base, entry.getKey());
			writeln(
				tagPath,
				"{" + NL
					+ TAB + "\"values\": [" + NL
					+ TAB + TAB + "\"" + getMinecraftId(functionNamesIterator.next()) + "\""
			);
			while (functionNamesIterator.hasNext()) {
				writeln(tagPath, "," + NL + TAB + TAB + "\"" + getMinecraftId(functionNamesIterator.next()) + "\"");
			}
			writeln(tagPath, TAB + "]" + NL + "}");
		}

		// Compile the functions of each unit
		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();
			Namespace craftlangNamespace = getCraftlangNamespace(namespace);

			for (FunctionDefinition function : unit.getFunctions()) {
				Path[] path = {getFunctionPath(base, new QualifiedName(namespace, function.getName()))};
				Scope<String, Variable> locals = new Scope<>();
				Deque<Variable> stack = new ArrayDeque<>();
				int[] helperCount = {0};

				// Summon the stack frame entity
				writeln(
					path[0],
					"summon minecraft:area_effect_cloud ~ ~ ~ {Tags:[\"cr_frame\"]}" + NL
						+ "execute as @e[tag=cr_frame] unless score @s cr_id matches 1.. store result score @s cr_id run scoreboard players add #cr cr_fp 1"
				);

				// Declare the variables for the function arguments and initialize the scores of the stack frame
				List<Parameter> parameters = function.getParameters();
				for (int i = 0, parameterCount = parameters.size(); i < parameterCount; i++) {
					Parameter parameter = parameters.get(i);
					String id = "cr_arg_" + Util.toBase62(i + 1);
					locals.declareAndAssign(parameter.getName(), new Variable(parameter.getType(), id));
					writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + id + " = #cr " + id);
				}

				// Declare the variable containing the returned value
				Type returnType = function.getReturnType();
				if (returnType != null) {
					locals.declareAndAssign(function.getName(), new Variable(returnType, "cr_return_1"));
				}

				// The expression compiler. Needed later
				ExpressionVisitor<Void, IOException> expressionCompiler = new ExpressionVisitor<Void, IOException>() {
					@Override
					public Void visitBinaryExpression(BinaryExpression expression) throws IOException {
						expression.getLeft().accept(this);
						expression.getRight().accept(this);
						Variable right = stack.pop();
						Variable left = stack.getFirst();
						Type type = left.getType();

						if (!right.getType().equals(type)) {
							throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
						}

						switch (type) {
							case BOOLEAN:
								switch (expression.getOperator()) {
									case AND:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + left.getId() + " *= @s " + right.getId());
										break;
									case OR:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + left.getId() + " unless entity @e[scores={" + left.getId() + "=0," + right.getId() + "=0}]");
										break;
									case EQUAL:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + left.getId() + " if score @s " + left.getId() + " = @s " + right.getId());
										break;
									case XOR:
									case NOT_EQUAL:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + left.getId() + " if score @s " + left.getId() + " != @s " + right.getId());
										break;
									default:
										throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case INTEGER:
								switch (expression.getOperator()) {
									case PLUS:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + left.getId() + " += @s " + right.getId());
										break;
									case MINUS:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + left.getId() + " -= @s " + right.getId());
										break;
									case TIMES:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + left.getId() + " *= @s " + right.getId());
										break;
									case DIVIDE:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + left.getId() + " /= @s " + right.getId());
										break;
									case REMAINDER:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + left.getId() + " %= @s " + right.getId());
										break;
									case EQUAL:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + left.getId() + " if score @s " + left.getId() + " = @s " + right.getId());
										left.setType(Type.BOOLEAN);
										break;
									case NOT_EQUAL:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + left.getId() + " if score @s " + left.getId() + " != @s " + right.getId());
										left.setType(Type.BOOLEAN);
										break;
									case LESS_OR_EQUAL:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + left.getId() + " if score @s " + left.getId() + " <= @s " + right.getId());
										left.setType(Type.BOOLEAN);
										break;
									case LESS:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + left.getId() + " if score @s " + left.getId() + " < @s " + right.getId());
										left.setType(Type.BOOLEAN);
										break;
									case GREATER_OR_EQUAL:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + left.getId() + " if score @s " + left.getId() + " >= @s " + right.getId());
										left.setType(Type.BOOLEAN);
										break;
									case GREATER:
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + left.getId() + " if score @s " + left.getId() + " > @s " + right.getId());
										left.setType(Type.BOOLEAN);
										break;
									default:
										throw new RuntimeException("Unsupported operation");
								}
								break;

							default:
								assert false : type;
								break;
						}

						return null;
					}

					@Override
					public Void visitUnaryExpression(UnaryExpression expression) throws IOException {
						expression.getOperand().accept(this);
						Variable operand = stack.pop();
						Type type = operand.getType();

						switch (expression.getOperator()) {
							case NOT:
								if (!type.equals(Type.BOOLEAN)) {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score @s " + operand.getId() + " if score @s " + operand.getId() + " matches 0");
								break;
							case PLUS:
								if (!type.equals(Type.INTEGER)) {
									throw new CompileException(expression.getSource().getEndIndex(), "Unsupported operation");
								}
								break;
							case MINUS:
								if (!type.equals(Type.INTEGER)) {
									throw new CompileException(expression.getSource().getEndIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + operand.getId() + " *= #cr cr_negate");
								break;
							default:
								assert false : expression.getOperator();
								break;
						}

						return null;
					}

					@Override
					public Void visitIntegerExpression(IntegerExpression expression) throws IOException {
						String id = "cr_stack_" + Util.toBase62(stack.size() + 1);
						writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players set @s " + id + " " + expression.getInteger());
						stack.push(new Variable(Type.INTEGER, id));
						maxStackCounts.put(namespace, Math.max(maxStackCounts.getOrDefault(namespace, 0), stack.size()));
						return null;
					}

					@Override
					public Void visitCommandExpression(CommandExpression expression) throws IOException {
						writeln(path[0], expression.getCommand());
						return null;
					}

					@Override
					public Void visitFunctionCallExpression(FunctionCallExpression expression) throws IOException {
						List<Argument> arguments = expression.getArguments();
						int argumentCount = arguments.size();
						QualifiedName functionName = expression.getFunctionName();
						if (functionName.getNamespace() == null) {
							functionName = new QualifiedName(namespace, functionName.getName());
						}
						FunctionDefinition function = functions.get(functionName);
						List<Parameter> parameters = function.getParameters();
						Type returnType = function.getReturnType();

						if (argumentCount != parameters.size()) {
							throw new CompileException(expression.getSource().getBeginIndex(), "Passed arguments don't match the function signature");
						}

						for (int i = 0; i < argumentCount; i++) {
							arguments.get(i).getExpression().accept(this);
							Variable argument = stack.pop();
							Type type = argument.getType();

							if (!type.equals(parameters.get(i).getType())) {
								throw new CompileException(expression.getSource().getBeginIndex(), "Passed arguments don't match the function signature");
							}

							switch (type) {
								case BOOLEAN:
								case INTEGER:
									writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation #cr cr_arg_" + Util.toBase62(i + 1) + " = @s " + argument.getId());
									break;
								default:
									assert false : type;
									break;
							}
						}

						writeln(path[0], "function " + getMinecraftId(functionName));

						if (returnType != null) {
							switch (returnType) {
								case BOOLEAN:
								case INTEGER:
									String id = "cr_stack_" + Util.toBase62(stack.size() + 1);
									writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + id + " = #cr cr_return_1");
									stack.push(new Variable(returnType, id));
									maxStackCounts.put(namespace, Math.max(maxStackCounts.getOrDefault(namespace, 0), stack.size()));
									break;
								default:
									assert false : returnType;
							}
						}
						return null;
					}

					@Override
					public Void visitVariableExpression(VariableExpression expression) throws IOException {
						QualifiedName name = expression.getName();
						Variable variable;
						boolean local;

						if (name.getNamespace() == null) {
							variable = locals.get(name.getName());

							if (variable != null) {
								local = true;
							} else {
								switch (name.getName()) {
									case "true":
										String id = "cr_stack_" + Util.toBase62(stack.size() + 1);
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players set @s " + id + " 1");
										stack.push(new Variable(Type.BOOLEAN, id));
										maxStackCounts.put(namespace, Math.max(maxStackCounts.getOrDefault(namespace, 0), stack.size()));
										return null;
									case "false":
										id = "cr_stack_" + Util.toBase62(stack.size() + 1);
										writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players set @s " + id + " 0");
										stack.push(new Variable(Type.BOOLEAN, id));
										maxStackCounts.put(namespace, Math.max(maxStackCounts.getOrDefault(namespace, 0), stack.size()));
										return null;
									default:
										variable = globals.get(new QualifiedName(namespace, name.getName()));
										local = false;
										break;
								}
							}
						} else {
							variable = globals.get(name);
							local = false;
						}

						if (variable == null) {
							throw new CompileException(expression.getSource().getBeginIndex(), "Undeclared variable " + name);
						}

						Type type = variable.getType();
						switch (type) {
							case BOOLEAN:
							case INTEGER:
								String id = "cr_stack_" + Util.toBase62(stack.size() + 1);
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + id + " = " + (local ? "@s " : "#cr ") + variable.getId());
								stack.push(new Variable(type, id));
								maxStackCounts.put(namespace, Math.max(maxStackCounts.getOrDefault(namespace, 0), stack.size()));
								break;
							default:
								assert false : type;
								break;
						}
						return null;
					}
				};

				// The statement compiler. Needed later
				StatementVisitor<Void, IOException> statementCompiler = new StatementVisitor<Void, IOException>() {
					@Override
					public Void visitVariableDeclarationAndAssignmentStatement(VariableDeclarationAndAssignmentStatement statement) throws IOException {
						String name = statement.getName();

						if (locals.get(name) != null) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Variable already defined: " + name);
						}

						Type expectedType = statement.getType();
						statement.getValue().accept(expressionCompiler);
						Variable value = stack.pop();
						Type type = value.getType();

						if (expectedType != null && !type.equals(expectedType)) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Assigned value doesn't match the expected type");
						}

						int localCount = maxLocalCounts.getOrDefault(namespace, 0) + 1;
						maxLocalCounts.put(namespace, localCount);
						String id = "cr_local_" + Util.toBase62(localCount);
						locals.declareAndAssign(name, new Variable(type, id));

						switch (type) {
							case BOOLEAN:
							case INTEGER:
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation @s " + id + " = @s " + value.getId());
								break;
							default:
								assert false : type;
								break;
						}
						return null;
					}

					@Override
					public Void visitVariableDeclarationStatement(VariableDeclarationStatement statement) {
						String name = statement.getName();

						if (locals.get(name) != null) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Variable already defined: " + name);
						}

						int localCount = maxLocalCounts.getOrDefault(namespace, 0) + 1;
						maxLocalCounts.put(namespace, localCount);
						locals.declareAndAssign(name, new Variable(statement.getType(), "cr_local_" + Util.toBase62(localCount)));
						return null;
					}

					@Override
					public Void visitVariableAssignmentStatement(VariableAssignmentStatement statement) throws IOException {
						QualifiedName name = statement.getName();
						Variable variable;
						boolean local;

						if (name.getNamespace() == null) {
							variable = locals.get(name.getName());
							if (variable != null) {
								local = true;
							} else {
								variable = globals.get(new QualifiedName(namespace, name.getName()));
								local = false;
							}
						} else {
							variable = globals.get(name);
							local = false;
						}

						if (variable == null) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Undeclared variable " + name);
						}

						statement.getValue().accept(expressionCompiler);
						Variable value = stack.pop();
						Type type = value.getType();

						if (!type.equals(variable.getType())) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Assigned value doesn't match the expected type");
						}

						switch (statement.getOperator()) {
							case EQUAL:
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation " + (local ? "@s " : "#cr ") + variable.getId() + " = @s " + value.getId());
								break;
							case PLUS_EQUAL:
								if (!type.equals(Type.INTEGER)) {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation " + (local ? "@s " : "#cr ") + variable.getId() + " += @s " + value.getId());
								break;
							case MINUS_EQUAL:
								if (!type.equals(Type.INTEGER)) {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation " + (local ? "@s " : "#cr ") + variable.getId() + " -= @s " + value.getId());
								break;
							case TIMES_EQUAL:
								if (!type.equals(Type.INTEGER)) {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation " + (local ? "@s " : "#cr ") + variable.getId() + " *= @s " + value.getId());
								break;
							case DIVIDE_EQUAL:
								if (!type.equals(Type.INTEGER)) {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation " + (local ? "@s " : "#cr ") + variable.getId() + " /= @s " + value.getId());
								break;
							case REMAINDER_EQUAL:
								if (!type.equals(Type.INTEGER)) {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation " + (local ? "@s " : "#cr ") + variable.getId() + " %= @s " + value.getId());
								break;
							case AND_EQUAL:
								if (!type.equals(Type.BOOLEAN)) {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation " + (local ? "@s " : "#cr ") + variable.getId() + " *= @s " + value.getId());
								break;
							case XOR_EQUAL:
								if (!type.equals(Type.BOOLEAN)) {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score " + (local ? "@s " : "#cr ") + variable.getId() + " if score @s " + variable.getId() + " != @s " + value.getId());
								break;
							case OR_EQUAL:
								if (!type.equals(Type.BOOLEAN)) {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp store success score " + (local ? "@s " : "#cr ") + variable.getId() + " unless entity @e[scores={" + variable.getId() + "=0," + value.getId() + "=0}]");
								break;
							default:
								assert false : statement.getOperator();
						}
						return null;
					}

					@Override
					public Void visitIfStatement(IfStatement statement) throws IOException {
						List<Statement> ifFalse = statement.getIfFalse();
						boolean hasFalse = !ifFalse.isEmpty();

						statement.getCondition().accept(expressionCompiler);
						Variable condition = hasFalse ? stack.getFirst() : stack.pop();

						if (!condition.getType().equals(Type.BOOLEAN)) {
							throw new CompileException(statement.getCondition().getSource().getBeginIndex(), "Not a boolean expression");
						}

						{
							QualifiedName helperName = new QualifiedName(craftlangNamespace, function.getName() + "." + Util.toBase62(++helperCount[0]));
							writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp if score @s " + condition.getId() + " matches 1 run function " + getMinecraftId(helperName));

							Path p = path[0];
							path[0] = getFunctionPath(base, helperName);

							for (Statement s : statement.getIfTrue()) {
								s.accept(this);
							}

							path[0] = p;
						}

						if (hasFalse) {
							stack.pop();
							QualifiedName helperName = new QualifiedName(craftlangNamespace, function.getName() + "." + Util.toBase62(++helperCount[0]));
							writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp if score @s " + condition.getId() + " matches 0 run function " + getMinecraftId(helperName));

							Path p = path[0];
							path[0] = getFunctionPath(base, helperName);

							for (Statement s : ifFalse) {
								s.accept(this);
							}

							path[0] = p;
						}
						return null;
					}

					@Override
					public Void visitWhileStatement(WhileStatement statement) throws IOException {
						QualifiedName helper1Name = new QualifiedName(craftlangNamespace, function.getName() + "." + Util.toBase62(++helperCount[0]));
						String helper1MinecraftId = getMinecraftId(helper1Name);
						writeln(path[0], "function " + helper1MinecraftId);

						Path p = path[0];
						path[0] = getFunctionPath(base, helper1Name);

						statement.getCondition().accept(expressionCompiler);
						Variable condition = stack.pop();
						if (!condition.getType().equals(Type.BOOLEAN)) {
							throw new CompileException(statement.getCondition().getSource().getBeginIndex(), "Not a boolean expression");
						}

						QualifiedName helper2Name = new QualifiedName(craftlangNamespace, function.getName() + "." + Util.toBase62(++helperCount[0]));
						writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp if score @s " + condition.getId() + " matches 1 run function " + getMinecraftId(helper2Name));
						path[0] = getFunctionPath(base, helper2Name);

						for (Statement s : statement.getBody()) {
							s.accept(this);
						}

						writeln(path[0], "function " + helper1MinecraftId);
						path[0] = p;
						return null;
					}

					@Override
					public Void visitDoWhileStatement(DoWhileStatement statement) throws IOException {
						QualifiedName helperName = new QualifiedName(craftlangNamespace, function.getName() + "." + Util.toBase62(++helperCount[0]));
						String helperMinecraftId = getMinecraftId(helperName);
						writeln(path[0], "function " + helperMinecraftId);

						Path p = path[0];
						path[0] = getFunctionPath(base, helperName);

						for (Statement s : statement.getBody()) {
							s.accept(this);
						}

						statement.getCondition().accept(expressionCompiler);
						Variable condition = stack.pop();
						if (!condition.getType().equals(Type.BOOLEAN)) {
							throw new CompileException(statement.getCondition().getSource().getBeginIndex(), "Not a boolean expression");
						}

						writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp if score @s " + condition.getId() + " matches 1 run function " + helperMinecraftId);
						path[0] = p;
						return null;
					}

					@Override
					public Void visitExpressionStatement(ExpressionStatement statement) throws IOException {
						statement.getExpression().accept(expressionCompiler);
						return null;
					}
				};

				// Compile the statements!
				for (Statement statement : function.getBody()) {
					statement.accept(statementCompiler);
				}

				// Kill the stack frame entity
				if (returnType != null) {
					writeln(path[0], "execute as @e if score @s cr_id = #cr cr_fp run scoreboard players operation #cr cr_return_1 = @s cr_return_1");
				}
				writeln(
					path[0],
					"execute as @e if score @s cr_id = #cr cr_fp run kill @s" + NL
						+ "scoreboard players remove #cr cr_fp 1"
				);
			}
		}

		// Generate the loader function for each unit, that is, a function which creates the objectives necessary to
		// pass arguments (cr_arg_<n>), keep local variables (cr_local_<n>) and return from a function (cr_return_1)
		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();
			Path path = getFunctionPath(base, new QualifiedName(getCraftlangNamespace(namespace), "load"));

			writeln(
				path,
				"gamerule maxCommandChainLength 2147483647" + NL
					+ "scoreboard objectives add cr_id dummy" + NL
					+ "scoreboard objectives add cr_fp dummy" + NL
					+ "scoreboard objectives add cr_negate dummy"
			);

			for (int i = 1, maxParameterCount = maxParameterCounts.getOrDefault(namespace, 0); i <= maxParameterCount; i++) {
				writeln(path, "scoreboard objectives add cr_arg_" + Util.toBase62(i) + " dummy");
			}

			for (int i = 1, maxLocalCount = maxLocalCounts.getOrDefault(namespace, 0); i <= maxLocalCount; i++) {
				writeln(path, "scoreboard objectives add cr_local_" + Util.toBase62(i) + " dummy");
			}

			for (int i = 1, maxValCount = maxStackCounts.getOrDefault(namespace, 0); i <= maxValCount; i++) {
				writeln(path, "scoreboard objectives add cr_stack_" + Util.toBase62(i) + " dummy");
			}

			writeln(
				path,
				"scoreboard objectives add cr_return_1 dummy" + NL
					+ "scoreboard players set #cr cr_id 0" + NL
					+ "scoreboard players set #cr cr_fp 0" + NL
					+ "scoreboard players set #cr cr_negate -1"
			);
		}
	}

	public static void compile(Path base, Unit... units) throws IOException {
		compile(base, Arrays.asList(units));
	}

	private static void writeln(Path path, String string) throws IOException {
		Files.write(
			path,
			(string + NL).getBytes(StandardCharsets.UTF_8),
			StandardOpenOption.CREATE,
			StandardOpenOption.WRITE,
			StandardOpenOption.APPEND
		);
	}

	private static Namespace getCraftlangNamespace(Namespace namespace) {
		List<String> craftlangNamespaceComponents = new ArrayList<>(namespace.getComponents());
		craftlangNamespaceComponents.add(".craftlang");
		return new Namespace(craftlangNamespaceComponents);
	}

	private static Path getFunctionPath(Path base, QualifiedName functionName) throws IOException {
		List<String> namespaceComponents = functionName.getNamespace().getComponents();
		Path path = base.resolve("data").resolve(namespaceComponents.get(0)).resolve("functions");
		for (int i = 1, namespaceComponentCount = namespaceComponents.size(); i < namespaceComponentCount; i++) {
			path = path.resolve(namespaceComponents.get(i));
		}
		Files.createDirectories(path);
		return path.resolve(functionName.getName() + ".mcfunction");
	}

	private static Path getTagPath(Path base, QualifiedName tag) throws IOException {
		List<String> namespaceComponents = tag.getNamespace().getComponents();
		Path path = base.resolve("data").resolve(namespaceComponents.get(0)).resolve("tags").resolve("functions");
		for (int i = 1, namespaceComponentCount = namespaceComponents.size(); i < namespaceComponentCount; i++) {
			path = path.resolve(namespaceComponents.get(i));
		}
		Files.createDirectories(path);
		return path.resolve(tag.getName() + ".json");
	}

	private static String getMinecraftId(QualifiedName qualifiedName) {
		List<String> namespaceComponents = qualifiedName.getNamespace().getComponents();
		StringBuilder result = new StringBuilder(namespaceComponents.get(0)).append(':');
		for (int i = 1, namespaceComponentCount = namespaceComponents.size(); i < namespaceComponentCount; i++) {
			result.append(namespaceComponents.get(i)).append('/');
		}
		return result.append(qualifiedName.getName()).toString();
	}

	public static class CompileException extends RuntimeException {
		private final int position;

		public CompileException(int position, String message) {
			super(message);
			this.position = position;
		}

		public int getPosition() {
			return position;
		}
	}

	private static class Variable implements Serializable {
		private Type type;
		private String id;

		public Variable(Type type, String id) {
			setType(type);
			setId(id);
		}

		public Type getType() {
			return type;
		}

		public void setType(Type type) {
			this.type = Objects.requireNonNull(type);
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = Objects.requireNonNull(id);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Variable)) {
				return false;
			}
			Variable variable = (Variable) obj;
			return variable.getType().equals(getType())
				&& variable.getId().equals(getId());
		}

		@Override
		public int hashCode() {
			return Objects.hash(
				getType(),
				getId()
			);
		}
	}
}