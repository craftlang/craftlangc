package dev.mamo.craftlangc;

import dev.mamo.craftlangc.ast.*;
import dev.mamo.craftlangc.ast.statement.*;
import dev.mamo.craftlangc.type.*;

import java.util.*;

public class Resolver {
	private Resolver() {}

	public static Map<FQN, Type> resolveTypes(Collection<Unit> units) {
		Map<FQN, Type> types = new LinkedHashMap<>();

		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();

			for (TypeDeclaration type : unit.getTypeDeclarations()) {
				types.put(new FQN(namespace, type.getName()), null);
			}
		}

		types.putIfAbsent(new FQN("bool"), PrimitiveType.BOOLEAN);
		types.putIfAbsent(new FQN("int"), PrimitiveType.INTEGER);

		List<Runnable> addMember = new ArrayList<>();
		List<Runnable> checkMember = new ArrayList<>();

		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();

			for (TypeDeclaration type : unit.getTypeDeclarations()) {
				Map<String, Type> members = new LinkedHashMap<>();

				for (TypeAndName member : type.getMembers()) {
					FQN[] memberTypeFQN = {member.getTypeFQN()};
					String memberName = memberTypeFQN[0].getName();

					if (!types.containsKey(memberTypeFQN[0])) {
						memberTypeFQN[0] = new FQN(namespace, memberName);
					}
					if (!types.containsKey(memberTypeFQN[0])) {
						throw new ResolveException(member.getSource().getBeginIndex(), "Undeclared type " + memberTypeFQN[0]);
					}

					addMember.add(() -> members.put(memberName, types.get(memberTypeFQN[0])));
					checkMember.add(() -> {
						if (members.get(memberName).isRecursive()) {
							throw new ResolveException(member.getSource().getBeginIndex(), "Recursive types are not allowed");
						}
					});
				}

				types.put(new FQN(namespace, type.getName()), new CompoundType(members));
			}
		}

		addMember.forEach(Runnable::run);
		checkMember.forEach(Runnable::run);

		return types;
	}

	public static Map<FQN, Store> resolveGlobals(Collection<Unit> units, Map<FQN, Type> types) {
		Map<FQN, Store> globals = new LinkedHashMap<>();

		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();

			for (VariableDeclarationStatement variableDeclaration : unit.getVariableDeclarations()) {
				FQN variableFQN = new FQN(namespace, variableDeclaration.getVariableName());

				if (globals.containsKey(variableFQN)) {
					throw new ResolveException(variableDeclaration.getSource().getBeginIndex(), "Variable already defined: " + variableFQN);
				}

				globals.put(variableFQN, new Store(types.get(variableDeclaration.getVariableTypeFQN()), variableFQN.hashCode()));
			}
		}

		return globals;
	}

	public static Map<FQN, FunctionDefinition> resolveFunctions(Collection<Unit> units) {
		Map<FQN, FunctionDefinition> functions = new LinkedHashMap<>();

		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();

			for (FunctionDefinition function : unit.getFunctionDefinitions()) {
				FQN functionFQN = new FQN(namespace, function.getName());

				if (functions.containsKey(functionFQN)) {
					throw new ResolveException(function.getSource().getBeginIndex(), "Function already defined: " + functionFQN);
				}

				functions.put(functionFQN, function);
			}
		}

		return functions;
	}

	public static Map<FQN, Set<FQN>> resolveTags(Collection<Unit> units) {
		Map<FQN, Set<FQN>> tags = new LinkedHashMap<>();

		for (Unit unit : units) {
			Namespace namespace = unit.getNamespace();

			for (FunctionDefinition function : unit.getFunctionDefinitions()) {
				List<FQN> functionTags = function.getTags();

				if (!functionTags.isEmpty()) {
					if (!function.getParameters().isEmpty()) {
						throw new ResolveException(function.getSource().getBeginIndex(), "Can't tag a function with a non-empty parameter list");
					}

					for (FQN tag : functionTags) {
						if (tag.getNamespace() == null) {
							tag = new FQN(new Namespace("minecraft"), tag.getName());
						}

						tags.putIfAbsent(tag, new LinkedHashSet<>());
						tags.get(tag).add(new FQN(namespace, function.getName()));
					}
				}
			}
		}

		return tags;
	}

	public static class ResolveException extends RuntimeException {
		private final int position;

		public ResolveException(int position, String message) {
			super(message);
			this.position = position;
		}

		public int getPosition() {
			return position;
		}
	}
}