package dev.mamo.craftlangc;

import dev.mamo.craftlangc.ast.*;
import dev.mamo.craftlangc.ast.expression.*;
import dev.mamo.craftlangc.ast.statement.*;
import dev.mamo.craftlangc.core.*;
import dev.mamo.craftlangc.type.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Map.*;

public class Compiler {
	private static final String NL = "\r\n";
	private static final String TAB = "\t";
	private static final boolean TRAILING_NL = false;
	private static final Map<Path, StringBuilder> BUFFERS = new LinkedHashMap<>();

	private Compiler() {}

	public static void compile(Path base, Collection<Unit> units) throws IOException {
		BUFFERS.clear();

		Map<FQN, Type> types = Resolver.resolveTypes(units);
		Map<FQN, Store> globals = Resolver.resolveGlobals(units, types);
		Map<FQN, FunctionDefinition> functions = Resolver.resolveFunctions(units);
		Map<FQN, Set<FQN>> tags = Resolver.resolveTags(units);
		Map<Namespace, Integer> maxAddresses = new LinkedHashMap<>();

		// TODO: resolve variables...

		for (Unit unit : units) {
			FQN loadTag = new FQN("minecraft", "load");
			tags.putIfAbsent(loadTag, new HashSet<>());
			tags.get(loadTag).add(new FQN(getCraftlangNamespace(unit.getNamespace()), "load"));
		}

		emit(
			base.resolve("pack.mcmeta"),
			"{",
			TAB + "\"pack\": {",
			TAB + TAB + "\"pack_format\": 4,",
			TAB + TAB + "\"description\": \"\"",
			TAB + '}',
			"}"
		);

		// Compile the functions of each unit
		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();
			Namespace craftlangNamespace = getCraftlangNamespace(namespace);

			for (FunctionDefinition function : unit.getFunctionDefinitions()) {
				String functionName = function.getName();
				Path[] path = {getFunctionPath(base, new FQN(namespace, functionName))};
				Scope<String, Store>[] locals = Utils.arrayOf(new Scope<>());
				int[] maxLocalAddress = {0};
				Deque<Store> stack = new ArrayDeque<>();
				int[] sp = {0};
				int[] helperCount = {0};

				// Summon the stack frame entity
				emit(
					path[0],
					"summon minecraft:area_effect_cloud ~ ~ ~ {Tags:[\"cr_frame\"]}",
					"execute as @e[tag=cr_frame] unless score @s cr_id matches -2147483648.. store result score @s cr_id run scoreboard players add #cr cr_fp 1"
				);

				// Declare the variables for the function arguments and initialize the scores of the stack frame
				for (TypeAndName parameter : function.getParameters()) {
					FQN typeFQN = parameter.getTypeFQN();
					Type type = types.get(typeFQN);
					if (type == null) {
						throw new CompileException(parameter.getSource().getBeginIndex(), "Unknown type: " + typeFQN);
					}

					String name = parameter.getName();
					if (locals[0].isDefined(name)) {
						throw new CompileException(parameter.getSource().getBeginIndex(), "Variable already declared: " + name);
					}

					locals[0].define(name, new Store(type, sp[0]));
					for (int i = 0, size = type.size(); i < size; i++) {
						asSFE(path[0], "run scoreboard players operation @s cr_" + sp[0] + " = #cr cr_" + sp[0]);
						sp[0]++;
					}
				}

				// Declare the variable containing the returned value
				FQN returnTypeFQN = function.getReturnTypeFQN();
				Type returnType = null;
				if (returnTypeFQN != null) {
					returnType = types.get(returnTypeFQN);
					locals[0].define(functionName, new Store(returnType, sp[0]));
					sp[0] += returnType.size();
				}

				// The expression compiler. Needed for the later defined statement compiler
				ExpressionVisitor<Void, RuntimeException> expressionCompiler = new ExpressionVisitor<Void, RuntimeException>() {
					@Override
					public Void visitBinaryExpression(BinaryExpression expression) {
						expression.getLeft().accept(this);
						expression.getRight().accept(this);
						Store right = stack.pop();
						Type type = right.getType();
						sp[0] -= type.size();
						Store left = stack.getFirst();

						if (!left.getType().equals(type)) {
							throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
						}

						switch (expression.getOperator()) {
							case PLUS:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation @s cr_" + left.getAddress() + " += @s cr_" + right.getAddress());
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case MINUS:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation @s cr_" + left.getAddress() + " -= @s cr_" + right.getAddress());
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case TIMES:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation @s cr_" + left.getAddress() + " *= @s cr_" + right.getAddress());
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case DIVIDE:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation @s cr_" + left.getAddress() + " /= @s cr_" + right.getAddress());
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case REMAINDER:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation @s cr_" + left.getAddress() + " %= @s cr_" + right.getAddress());
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case AND:
								if (type.equals(PrimitiveType.BOOLEAN)) {
									asSFE(path[0], "run scoreboard players operation @s cr_" + left.getAddress() + " *= @s cr_" + right.getAddress());
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case XOR:
								if (type.equals(PrimitiveType.BOOLEAN)) {
									asSFE(path[0], "store success score @s cr_" + left.getAddress() + " if score @s cr_" + left.getAddress() + " != @s cr_" + right.getAddress());
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case OR:
								if (type.equals(PrimitiveType.BOOLEAN)) {
									asSFE(path[0], "store success score @s cr_" + left.getAddress() + " unless entity @e[scores={cr_" + left.getAddress() + "=0,cr_" + right.getAddress() + "=0}]");
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case EQUAL:
								if (type.equals(PrimitiveType.BOOLEAN)) {
									asSFE(path[0], "store success score @s cr_" + left.getAddress() + " if score @s cr_" + left.getAddress() + " = @s cr_" + right.getAddress());
								} else if (type.equals(PrimitiveType.INTEGER)) {
									int leftAddress = left.getAddress();
									asSFE(path[0], "store success score @s cr_" + leftAddress + " if score @s cr_" + leftAddress + " = @s cr_" + right.getAddress());
									left.setType(PrimitiveType.BOOLEAN);
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case NOT_EQUAL:
								if (type.equals(PrimitiveType.BOOLEAN)) {
									asSFE(path[0], "store success score @s cr_" + left.getAddress() + " if score @s cr_" + left.getAddress() + " != @s cr_" + right.getAddress());
								} else if (type.equals(PrimitiveType.INTEGER)) {
									int leftAddress = left.getAddress();
									asSFE(path[0], "store success score @s cr_" + leftAddress + " if score @s cr_" + leftAddress + " != @s cr_" + right.getAddress());
									left.setType(PrimitiveType.BOOLEAN);
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case LESS_OR_EQUAL:
								if (type.equals(PrimitiveType.INTEGER)) {
									int leftAddress = left.getAddress();
									asSFE(path[0], "store success score @s cr_" + leftAddress + " if score @s cr_" + leftAddress + " <= @s cr_" + right.getAddress());
									left.setType(PrimitiveType.BOOLEAN);
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case LESS:
								if (type.equals(PrimitiveType.INTEGER)) {
									int leftAddress = left.getAddress();
									asSFE(path[0], "store success score @s cr_" + leftAddress + " if score @s cr_" + leftAddress + " < @s cr_" + right.getAddress());
									left.setType(PrimitiveType.BOOLEAN);
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case GREATER_OR_EQUAL:
								if (type.equals(PrimitiveType.INTEGER)) {
									int leftAddress = left.getAddress();
									asSFE(path[0], "store success score @s cr_" + leftAddress + " if score @s cr_" + leftAddress + " >= @s cr_" + right.getAddress());
									left.setType(PrimitiveType.BOOLEAN);
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case GREATER:
								if (type.equals(PrimitiveType.INTEGER)) {
									int leftAddress = left.getAddress();
									asSFE(path[0], "store success score @s cr_" + leftAddress + " if score @s cr_" + leftAddress + " > @s cr_" + right.getAddress());
									left.setType(PrimitiveType.BOOLEAN);
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							default:
								assert false : expression.getOperator();
						}

						return null;
					}

					@Override
					public Void visitUnaryExpression(UnaryExpression expression) {
						expression.getOperand().accept(this);
						Store operand = stack.getFirst();
						Type type = operand.getType();

						switch (expression.getOperator()) {
							case NOT:
								if (type.equals(PrimitiveType.BOOLEAN)) {
									int operandAddress = operand.getAddress();
									asSFE(path[0], "store success score @s cr_" + operandAddress + " if score @s cr_" + operandAddress + " matches 0");
								} else {
									throw new CompileException(expression.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case PLUS:
								if (!type.equals(PrimitiveType.INTEGER)) {
									throw new CompileException(expression.getSource().getEndIndex(), "Unsupported operation");
								}
								break;

							case MINUS:
								if (type.equals(PrimitiveType.INTEGER)) {
									int tempAddress = sp[0] + 1;
									emit(
										path[0],
										"execute as @e[tag=cr_frame] if score @s cr_id = #cr cr_fp run scoreboard players set @s cr_" + tempAddress + " -1",
										"execute as @e[tag=cr_frame] if score @s cr_id = #cr cr_fp run scoreboard players operation @s cr_" + operand.getAddress() + " *= @s cr_" + tempAddress
									);
									maxAddresses.put(namespace, Math.max(maxAddresses.getOrDefault(namespace, 0), tempAddress));
								} else {
									throw new CompileException(expression.getSource().getEndIndex(), "Unsupported operation");
								}
								break;

							default:
								assert false : expression.getOperator();
								break;
						}

						return null;
					}

					@Override
					public Void visitIntegerExpression(IntegerExpression expression) {
						asSFE(path[0], "run scoreboard players set @s cr_" + sp[0] + ' ' + expression.getValue());
						stack.push(new Store(PrimitiveType.INTEGER, sp[0]));
						maxAddresses.put(namespace, Math.max(maxAddresses.getOrDefault(namespace, 0), ++sp[0]));
						return null;
					}

					@Override
					public Void visitCommandExpression(CommandExpression expression) {
						emit(path[0], expression.getCommand());
						return null;
					}

					@Override
					public Void visitFunctionCallExpression(CallExpression expression) {
						FQN functionFQN = expression.getFunctionFQN();
						if (functionFQN.getNamespace() == null) {
							functionFQN = new FQN(namespace, functionFQN.getName());
						}

						if (!functions.containsKey(functionFQN)) {
							throw new CompileException(expression.getSource().getBeginIndex(), "Unknown function: " + functionFQN);
						}

						FunctionDefinition function = functions.get(functionFQN);
						FQN returnTypeFQN = function.getReturnTypeFQN();
						List<TypeAndName> parameters = function.getParameters();
						List<Expression> arguments = expression.getArguments();

						if (arguments.size() != parameters.size()) {
							throw new CompileException(expression.getSource().getBeginIndex(), "Passed arguments don't match the function signature");
						}

						Iterator<Type> parameterIterator = parameters.stream().map(p -> types.get(p.getTypeFQN())).iterator();
						int address = 0;
						for (Expression argumentExpression : arguments) {
							argumentExpression.accept(this);
							Store argument = stack.pop();
							Type type = argument.getType();
							sp[0] -= type.size();

							if (!type.equals(parameterIterator.next())) {
								throw new CompileException(expression.getSource().getBeginIndex(), "Passed arguments don't match the function signature");
							}

							int argumentAddress = argument.getAddress();
							for (int i = 0, typeSize = type.size(); i < typeSize; i++) {
								asSFE(path[0], "run scoreboard players operation #cr cr_" + (address + i) + " = @s cr_" + (argumentAddress + i));
							}
							address += type.size();
						}

						emit(path[0], "function " + getMinecraftId(functionFQN));

						if (returnTypeFQN != null && !returnTypeFQN.equals(new FQN("void"))) {
							Type returnType = types.get(returnTypeFQN);
							stack.push(new Store(returnType, sp[0]));
							for (int i = 0, size = returnType.size(); i < size; i++) {
								asSFE(path[0], "run scoreboard players operation @s cr_" + sp[0] + " = #cr cr_" + i);
								sp[0]++;
							}
						}

						return null;
					}

					@Override
					public Void visitVariableExpression(VariableExpression expression) {
						FQN variableFQN = expression.getFQN();
						boolean[] local = {variableFQN.getNamespace() == null};
						Store store = local[0] ? locals[0].get(variableFQN.getName()) : globals.get(variableFQN);

						if (store == null && local[0]) {
							globals.get(new FQN(namespace, variableFQN.getName()));
							local[0] = false;
						}

						if (store == null) {
							switch (variableFQN.getName()) {
								case "true":
									int address = stack.size();
									asSFE(path[0], "run scoreboard players set @s cr_" + address + " 1");
									stack.push(new Store(PrimitiveType.BOOLEAN, address));
									maxAddresses.put(namespace, Math.max(maxAddresses.getOrDefault(namespace, 0), stack.size()));
									return null;

								case "false":
									address = stack.size();
									asSFE(path[0], "run scoreboard players set @s cr_" + address + " 0");
									stack.push(new Store(PrimitiveType.BOOLEAN, address));
									maxAddresses.put(namespace, Math.max(maxAddresses.getOrDefault(namespace, 0), stack.size()));
									return null;

								default:
									throw new CompileException(expression.getSource().getBeginIndex(), "Unknown variable " + variableFQN);
							}
						}

						Type type = store.getType();
						stack.push(new Store(type, sp[0]));
						for (int i = 0, typeSize = type.size(); i < typeSize; i++) {
							asSFE(path[0], "run scoreboard players operation @s cr_" + sp[0] + " = " + (local[0] ? "@s" : "#cr") + " cr_" + store.getAddress());
							sp[0]++;
						}
						maxAddresses.put(namespace, Math.max(maxAddresses.getOrDefault(namespace, 0), sp[0]));

						return null;
					}
				};

				// The statement compiler. Needed later
				StatementVisitor<Void, RuntimeException> statementCompiler = new StatementVisitor<Void, RuntimeException>() {
					@Override
					public Void visitVariableDeclarationAndAssignmentStatement(VariableDeclarationAndAssignmentStatement statement) {
						String variableName = statement.getVariableName();
						if (locals[0].isDefined(variableName)) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Variable already declared: " + variableName);
						}

						FQN expectedTypeFQN = statement.getVariableTypeFQN();
						if (expectedTypeFQN != null && !types.containsKey(expectedTypeFQN)) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Unknown type: " + expectedTypeFQN);
						}

						Type expectedType = types.get(expectedTypeFQN);
						statement.getAssignedValue().accept(expressionCompiler);
						Store value = stack.getFirst();
						Type type = value.getType();

						if (expectedType != null && !type.equals(expectedType)) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Assigned value doesn't match the expected type");
						}

						locals[0].define(variableName, new Store(type, value.getAddress()));

						return null;
					}

					@Override
					public Void visitVariableDeclarationStatement(VariableDeclarationStatement statement) {
						String name = statement.getVariableName();
						if (locals[0].isDefined(name)) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Variable already declared: " + name);
						}

						FQN typeFQN = statement.getVariableTypeFQN();
						if (!types.containsKey(typeFQN)) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Unknown type: " + typeFQN);
						}

						Type type = types.get(typeFQN);
						locals[0].define(name, new Store(type, sp[0]));
						sp[0] += type.size();
						maxAddresses.put(namespace, Math.max(maxAddresses.getOrDefault(namespace, 0), sp[0]));
						maxLocalAddress[0] = Math.max(maxLocalAddress[0], sp[0]);

						return null;
					}

					@Override
					public Void visitVariableAssignmentStatement(VariableAssignmentStatement statement) {
						FQN variableFQN = statement.getVariableFQN();
						Store store;
						String player;

						if (variableFQN.getNamespace() == null) {
							store = locals[0].get(variableFQN.getName());
							if (store != null) {
								player = "@s";
							} else {
								store = globals.get(new FQN(namespace, variableFQN.getName()));
								player = "#cr";
							}
						} else {
							store = globals.get(variableFQN);
							player = "#cr";
						}

						if (store == null) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Undeclared variable " + variableFQN);
						}

						sp[0] = maxLocalAddress[0] + 1;
						statement.getValue().accept(expressionCompiler);
						Store value = stack.pop();
						Type type = value.getType();
						sp[0] = 0;

						if (!type.equals(store.getType())) {
							throw new CompileException(statement.getSource().getBeginIndex(), "Assigned value doesn't match the expected type");
						}

						switch (statement.getOperator()) {
							case EQUAL:
								asSFE(path[0], "run scoreboard players operation " + player + " cr_" + store.getAddress() + " = @s cr_" + value.getAddress());
								break;

							case PLUS_EQUAL:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation " + player + " cr_" + store.getAddress() + " += @s cr_" + value.getAddress());
								} else {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case MINUS_EQUAL:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation " + player + " cr_" + store.getAddress() + " -= @s cr_" + value.getAddress());
								} else {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case TIMES_EQUAL:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation " + player + " cr_" + store.getAddress() + " *= @s cr_" + value.getAddress());
								} else {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case DIVIDE_EQUAL:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation " + player + " cr_" + store.getAddress() + " /= @s cr_" + value.getAddress());
								} else {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case REMAINDER_EQUAL:
								if (type.equals(PrimitiveType.INTEGER)) {
									asSFE(path[0], "run scoreboard players operation " + player + " cr_" + store.getAddress() + " %= @s cr_" + value.getAddress());
								} else {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case AND_EQUAL:
								if (type.equals(PrimitiveType.BOOLEAN)) {
									asSFE(path[0], "run scoreboard players operation " + player + " cr_" + store.getAddress() + " *= @s cr_" + value.getAddress());
								} else {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case XOR_EQUAL:
								if (type.equals(PrimitiveType.BOOLEAN)) {
									asSFE(path[0], "store success score " + player + " cr_" + store.getAddress() + " if score @s cr_" + store.getAddress() + " != @s cr_" + value.getAddress());
								} else {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							case OR_EQUAL:
								if (type.equals(PrimitiveType.BOOLEAN)) {
									asSFE(path[0], "store success score " + player + " cr_" + store.getAddress() + " unless entity @e[scores={cr_" + store.getAddress() + "=0,cr_" + value.getAddress() + "=0}]");
								} else {
									throw new CompileException(statement.getSource().getBeginIndex(), "Unsupported operation");
								}
								break;

							default:
								assert false : statement.getOperator();
						}

						return null;
					}

					@Override
					public Void visitIfStatement(IfStatement statement) {
						List<Statement> falseBranch = statement.getFalseBranch();
						boolean hasFalseBranch = !falseBranch.isEmpty();

						Store condition;
						Type conditionType;
						statement.getCondition().accept(expressionCompiler);
						if (hasFalseBranch) {
							condition = stack.getFirst();
							conditionType = condition.getType();
						} else {
							condition = stack.pop();
							conditionType = condition.getType();
							sp[0] -= conditionType.size();
						}

						if (!conditionType.equals(PrimitiveType.BOOLEAN)) {
							throw new CompileException(statement.getCondition().getSource().getBeginIndex(), "Not a boolean expression");
						}

						{
							FQN helperFQN = new FQN(craftlangNamespace, function.getName() + '.' + helperCount[0]++);
							asSFE(path[0], "if score @s cr_" + condition.getAddress() + " matches 1 run function " + getMinecraftId(helperFQN));

							Path p = path[0];
							path[0] = getFunctionPath(base, helperFQN);
							locals[0] = new Scope<>(locals[0]);

							for (Statement s : statement.getTrueBranch()) {
								s.accept(this);
							}

							locals[0] = locals[0].getParent();
							path[0] = p;
						}

						if (hasFalseBranch) {
							sp[0] -= stack.pop().getType().size();
							FQN helperFQN = new FQN(craftlangNamespace, function.getName() + '.' + helperCount[0]++);
							asSFE(path[0], "if score @s cr_" + condition.getAddress() + " matches 0 run function " + getMinecraftId(helperFQN));

							Path p = path[0];
							path[0] = getFunctionPath(base, helperFQN);
							locals[0] = new Scope<>(locals[0]);

							for (Statement s : falseBranch) {
								s.accept(this);
							}

							locals[0] = locals[0].getParent();
							path[0] = p;
						}

						return null;
					}

					@Override
					public Void visitWhileStatement(WhileStatement statement) {
						FQN helper1FQN = new FQN(craftlangNamespace, function.getName() + '.' + helperCount[0]++);
						String helper1MinecraftId = getMinecraftId(helper1FQN);
						emit(path[0], "function " + helper1MinecraftId);

						Path p = path[0];
						path[0] = getFunctionPath(base, helper1FQN);

						statement.getCondition().accept(expressionCompiler);
						Store condition = stack.pop();
						Type conditionType = condition.getType();
						sp[0] -= conditionType.size();
						if (!conditionType.equals(PrimitiveType.BOOLEAN)) {
							throw new CompileException(statement.getCondition().getSource().getBeginIndex(), "Not a boolean expression");
						}

						FQN helper2FQN = new FQN(craftlangNamespace, function.getName() + '.' + helperCount[0]++);
						asSFE(path[0], "if score @s cr_" + condition.getAddress() + " matches 1 run function " + getMinecraftId(helper2FQN));
						path[0] = getFunctionPath(base, helper2FQN);
						locals[0] = new Scope<>(locals[0]);

						for (Statement s : statement.getBody()) {
							s.accept(this);
						}

						locals[0] = locals[0].getParent();
						emit(path[0], "function " + helper1MinecraftId);
						path[0] = p;

						return null;
					}

					@Override
					public Void visitDoWhileStatement(DoWhileStatement statement) {
						FQN helperFQN = new FQN(craftlangNamespace, function.getName() + '.' + helperCount[0]++);
						String helperMinecraftId = getMinecraftId(helperFQN);
						emit(path[0], "function " + helperMinecraftId);

						Path p = path[0];
						path[0] = getFunctionPath(base, helperFQN);
						locals[0] = new Scope<>(locals[0]);

						for (Statement s : statement.getBody()) {
							s.accept(this);
						}

						locals[0] = locals[0].getParent();

						statement.getCondition().accept(expressionCompiler);
						Store condition = stack.pop();
						Type conditionType = condition.getType();
						sp[0] -= conditionType.size();
						if (!conditionType.equals(PrimitiveType.BOOLEAN)) {
							throw new CompileException(statement.getCondition().getSource().getBeginIndex(), "Not a boolean expression");
						}

						asSFE(path[0], "if score @s cr_" + condition.getAddress() + " matches 1 run function " + helperMinecraftId);
						path[0] = p;

						return null;
					}

					@Override
					public Void visitExpressionStatement(ExpressionStatement statement) {
						statement.getExpression().accept(expressionCompiler);
						stack.poll();
						sp[0] = 0;
						return null;
					}
				};

				// Compile the statements!
				for (Statement statement : function.getBody()) {
					statement.accept(statementCompiler);
				}

				// Kill the stack frame entity
				if (returnType != null) {
					int address = locals[0].get(functionName).getAddress();
					for (int i = 0, returnTypeSize = returnType.size(); i < returnTypeSize; i++) {
						asSFE(path[0], "run scoreboard players operation #cr cr_" + i + " = @s cr_" + (address + i));
					}
				}
				asSFE(path[0], "run kill @s");
				emit(path[0], "scoreboard players remove #cr cr_fp 1");
			}
		}

		// Generate the loader function for each unit, that is, a function which creates the objectives necessary to
		// pass arguments, keep local variables and return from a function
		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();
			Path path = getFunctionPath(base, new FQN(getCraftlangNamespace(namespace), "load"));

			emit(
				path,
				"gamerule maxCommandChainLength 2147483647",
				"scoreboard objectives add cr_id dummy",
				"scoreboard objectives add cr_fp dummy"
			);

			for (Store global : globals.values()) {
				emit(path, "scoreboard objectives add cr_" + global.getAddress() + " dummy");
			}

			for (int i = 0, maxAddress = maxAddresses.getOrDefault(namespace, 0); i < maxAddress; i++) {
				emit(path, "scoreboard objectives add cr_" + i + " dummy");
			}

			emit(path, "scoreboard players set #cr cr_fp -1");
		}

		// Generate the JSON tag files for each function
		for (Map.Entry<FQN, Set<FQN>> entry : tags.entrySet()) {
			Iterator<FQN> functionFQNIterator = entry.getValue().iterator();
			if (!functionFQNIterator.hasNext()) {
				continue;
			}

			StringBuilder json = new StringBuilder("{" + NL + TAB + "\"values\": [");
			json.append(NL + TAB + TAB + '"').append(getMinecraftId(functionFQNIterator.next())).append('"');
			while (functionFQNIterator.hasNext()) {
				json.append(',' + NL + TAB + TAB + '"').append(getMinecraftId(functionFQNIterator.next())).append('"');
			}
			json.append(NL + TAB + ']' + NL + '}');

			emit(getTagPath(base, entry.getKey()), json.toString());
		}

		flush();
	}

	private static void emit(Path path, String... lines) {
		BUFFERS.putIfAbsent(path, new StringBuilder());
		StringBuilder buffer = BUFFERS.get(path);
		for (String line : lines) {
			buffer.append(line).append(NL);
		}
	}

	private static void asSFE(Path path, String... commands) {
		for (String command : commands) {
			emit(path, "execute as @e[tag=cr_frame] if score @s cr_id = #cr cr_fp " + command);
		}
	}

	private static void flush() throws IOException {
		int nlLength = NL.length();

		for (Entry<Path, StringBuilder> entry : BUFFERS.entrySet()) {
			Path path = entry.getKey();
			StringBuilder buffer = entry.getValue();
			int bufferLength = buffer.length();

			if (!TRAILING_NL && buffer.lastIndexOf(NL) == bufferLength - nlLength) {
				buffer.delete(bufferLength - nlLength, bufferLength);
			}

			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			Files.write(path, Arrays.asList(buffer));
		}

		BUFFERS.clear();
	}

	private static Namespace getCraftlangNamespace(Namespace namespace) {
		List<String> craftlangNamespaceComponents = new ArrayList<>(namespace.getComponents());
		craftlangNamespaceComponents.add(".craftlang");
		return new Namespace(craftlangNamespaceComponents);
	}

	private static Path getFunctionPath(Path base, FQN functionFQN) {
		Iterator<String> namespaceComponentsIterator = functionFQN.getNamespace().getComponents().iterator();
		Path path = base.resolve("data").resolve(namespaceComponentsIterator.next()).resolve("functions");
		while (namespaceComponentsIterator.hasNext()) {
			path = path.resolve(namespaceComponentsIterator.next());
		}
		return path.resolve(functionFQN.getName() + ".mcfunction");
	}

	private static Path getTagPath(Path base, FQN tag) throws IOException {
		List<String> namespaceComponents = tag.getNamespace().getComponents();
		Path path = base.resolve("data").resolve(namespaceComponents.get(0)).resolve("tags").resolve("functions");
		for (int i = 1, namespaceComponentCount = namespaceComponents.size(); i < namespaceComponentCount; i++) {
			path = path.resolve(namespaceComponents.get(i));
		}
		Files.createDirectories(path);
		return path.resolve(tag.getName() + ".json");
	}

	private static String getMinecraftId(FQN FQN) {
		Iterator<String> namespaceComponentsIterator = FQN.getNamespace().getComponents().iterator();
		StringBuilder result = new StringBuilder(namespaceComponentsIterator.next()).append(':');
		while (namespaceComponentsIterator.hasNext()) {
			result.append(namespaceComponentsIterator.next()).append('/');
		}
		return result.append(FQN.getName()).toString();
	}

	public static class CompileException extends RuntimeException {
		private final int position;

		private CompileException(int position, String message) {
			super(message);
			this.position = position;
		}

		public int getPosition() {
			return position;
		}
	}
}