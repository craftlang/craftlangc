__author__ = 'Matteo Morena'
__all__ = ('compile_file', 'compile_func_def', 'compile_statement', 'compile_expr')

from dataclasses import dataclass
from itertools import chain, islice
from os import makedirs
from os.path import dirname, join
from typing import Iterator, List, TextIO
from unicodedata import normalize

from craftlangc.cst import Arg, AssignStatement, BinaryExpr, CommandStatement, DoWhileStatement, Expr, File, FuncCall, \
	FuncDef, IdentifierExpr, IfStatement, LiteralExpr, NopStatement, ParensExpr, ReturnStatement, Statement, \
	SwapStatement, Token, UnaryExpr, WhileStatement
from craftlangc.enums import VarType
from craftlangc.scope import Scope


def compile_file(file: File, out_dir: str) -> None:
	nc = file.namespace_decl.components

	makedirs(out_dir, exist_ok = True)
	with open(join(out_dir, 'pack.mcmeta'), 'wt', encoding = 'UTF-8', newline = '') as out:
		out.write(
			'{\r\n'
			'\t"pack": {\r\n'
			'\t\t"pack_format": 4,\r\n'
			'\t\t"description": ""\r\n'
			'\t}\r\n'
			'}\r\n'
		)

	f = join(
		out_dir, "data", _asciify(str(nc[0])), "functions", *map(lambda c: _asciify(str(c)), islice(nc, 1, None)),
		'.load.mcfunction'
	)
	makedirs(dirname(f), exist_ok = True)
	with open(f, 'wt', encoding = 'UTF-8', newline = '') as out:
		out.write(
			'gamerule maxCommandChainLength 2147483647\r\n'
			'scoreboard objectives add craftlang dummy\r\n'
		)

	f = join(out_dir, 'data', 'minecraft', 'tags', 'functions', 'load.json')
	makedirs(dirname(f), exist_ok = True)
	with open(f, 'wt', encoding = 'UTF-8', newline = '') as out:
		out.write(
			'{\r\n'
			'\t"values": [\r\n'
			f'\t\t"{_asciify(str(nc[0]))}:{"/".join(map(lambda c: _asciify(str(c)), islice(nc, 1, None)))}/.load"\r\n'
			'\t]\r\n'
			'}\r\n'
		)

	for func_def in file.func_defs:
		compile_func_def(file, func_def, out_dir)


def compile_func_def(file: File, func_def: FuncDef, out_dir: str) -> None:
	# TODO: Test for recursion

	if func_def not in file.func_defs:
		raise Exception()  # TODO

	def aux_gen_t() -> Iterator[Auxiliary]:
		i = -1
		while True:
			i += 1
			nc = file.namespace_decl.components
			yield Auxiliary(
				f'''{_asciify(str(nc[0]))}:{"/".join([
					*map(lambda c: _asciify(str(c)), islice(nc, 1, None)), _asciify(str(func_def.identifier))
				])}.{i}''',
				open(f'''{join(
					out_dir, "data", _asciify(str(file.namespace_decl.components[0])), "functions",
					*map(lambda c: _asciify(str(c)), islice(file.namespace_decl.components, 1, None)),
					_asciify(str(func_def.identifier))
				)}.{i}.mcfunction''', 'wt', encoding = 'UTF-8', newline = '')
			)

	def iid_gen_t() -> Iterator[str]:
		i = -1
		while True:
			i += 1
			yield (
				f'locals.{".".join(map(lambda c: _asciify(str(c)), file.namespace_decl.components))}'
				f'.{_asciify(str(func_def.identifier))}.{i}'
			)

	aux_gen = aux_gen_t()
	iid_gen = iid_gen_t()
	scope = Scope()
	for i, parameter in enumerate(func_def.params):
		scope.set(
			str(parameter.identifier),
			Scope.Item(
				f'args.{".".join(map(lambda c: _asciify(str(c)), file.namespace_decl.components))}'
				f'.{_asciify(str(func_def.identifier))}.{i}', VarType.from_str(str(parameter.type))
			)
		)

	path = f'''{join(
		out_dir, "data", _asciify(str(file.namespace_decl.components[0])), "functions",
		*map(lambda c: _asciify(str(c)), islice(file.namespace_decl.components, 1, None)),
		_asciify(str(func_def.identifier))
	)}.mcfunction'''
	makedirs(dirname(path), exist_ok = True)

	with open(path, 'wt', encoding = 'UTF-8', newline = '') as out:
		out.write(f'# {func_def.identifier}({", ".join(map(str, func_def.params))}): {func_def.return_type}\r\n')

		for s in func_def.statements:
			out.write('\r\n')
			compile_statement(
				file, s, out, aux_gen, scope, iid_gen,
				f'rets.{".".join(map(lambda c: _asciify(str(c)), file.namespace_decl.components))}'
				f'.{_asciify(str(func_def.identifier))}.0',
				VarType.from_str(str(func_def.return_type))
			)


def compile_statement(
		file: File, statement: Statement, out: TextIO, aux_gen: Iterator['Auxiliary'], scope: Scope,
		iid_gen: Iterator[str], ret: str, ret_type: VarType
) -> None:
	stack: List[VarType] = []

	if isinstance(statement, NopStatement):
		out.write(f'# {statement}\r\n')

	elif isinstance(statement, CommandStatement):
		out.write(f'# {statement}\r\n')

		for component in reversed(statement.components):
			if isinstance(component, Arg):
				compile_expr(file, component.expr, out, scope, stack)

		for component in statement.components:
			if isinstance(component, Token):
				out.write(str(component))
			elif isinstance(component, Arg):
				if component.by_ref:
					out.write(_scope_get(scope, str(component.expr)).iid)
				else:
					stack.pop()
					out.write(f'stack.{len(stack)}')
			else:
				raise Exception()  # TODO

		out.write('\r\n')

	elif isinstance(statement, SwapStatement):
		out.write(f'# {statement}\r\n')

		left = _scope_get(scope, str(statement.left))
		right = _scope_get(scope, str(statement.right))

		if (
				(left.type == VarType.BOOLEAN and right.type == VarType.BOOLEAN) or
				(left.type == VarType.SCORE and right.type == VarType.SCORE)
		):
			out.write(f'scoreboard players operation {left.iid} craftlang >< {right.iid} craftlang\r\n')
		elif left.type == VarType.ENTITY and right.type == VarType.ENTITY:
			out.write(
				f'tag @e remove stack.{len(stack)}\r\n'
				f'tag @e[tag={left.iid}] add stack.{len(stack)}\r\n'
				f'tag @e remove {left.iid}\r\n'
				f'tag @e[tag={right.iid}] add {left.iid}\r\n'
				f'tag @e remove {right.iid}\r\n'
				f'tag @e[tag=stack.{len(stack)}] add {right.iid}\r\n'
			)
		else:
			raise Exception()  # TODO

	elif isinstance(statement, AssignStatement):
		out.write(f'# {statement}\r\n')

		compile_expr(file, statement.expr, out, scope, stack)
		local = str(statement.identifier)
		operator = str(statement.operator)
		type = stack.pop()

		if operator == '=':
			if type in {VarType.BOOLEAN, VarType.SCORE}:
				item = scope.set(local, type, iid_gen)
				out.write(f'scoreboard players operation {item.iid} craftlang = stack.{len(stack)} craftlang\r\n')
			elif type == VarType.ENTITY:
				item = scope.set(local, VarType.ENTITY, iid_gen)
				out.write(
					f'tag @e remove {item.iid}\r\n'
					f'tag @e[tag=stack.{len(stack)}] add {item.iid}\r\n'
				)
			else:
				raise Exception()  # TODO

		elif operator == '*=':
			item = _scope_get(scope, local)
			if item.type == VarType.SCORE and type == VarType.SCORE:
				out.write(f'scoreboard players operation {item.iid} craftlang *= stack.{len(stack)} craftlang\r\n')
			else:
				raise Exception()  # TODO

		elif operator == '/=':
			item = _scope_get(scope, local)
			if item.type == VarType.SCORE and type == VarType.SCORE:
				out.write(f'scoreboard players operation {item.iid} craftlang /= stack.{len(stack)} craftlang\r\n')
			else:
				raise Exception()  # TODO

		elif operator == '%=':
			item = _scope_get(scope, local)
			if item.type == VarType.SCORE and type == VarType.SCORE:
				out.write(f'scoreboard players operation {item.iid} craftlang %= stack.{len(stack)} craftlang\r\n')
			else:
				raise Exception()  # TODO

		elif operator == '+=':
			item = _scope_get(scope, local)
			if item.type == VarType.SCORE and type == VarType.SCORE:
				out.write(f'scoreboard players operation {item.iid} craftlang += stack.{len(stack)} craftlang\r\n')
			else:
				raise Exception()  # TODO

		elif operator == '-=':
			item = _scope_get(scope, local)
			if item.type == VarType.SCORE and type == VarType.SCORE:
				out.write(f'scoreboard players operation {item.iid} craftlang -= stack.{len(stack)} craftlang\r\n')
			elif item.type == VarType.ENTITY and type == VarType.ENTITY:
				out.write(f'tag @e[tag=stack.{len(stack)}] remove {item.iid}\r\n')
			else:
				raise Exception()  # TODO

		elif operator == '&=':
			item = _scope_get(scope, local)
			if item.type == VarType.BOOLEAN and type == VarType.BOOLEAN:
				out.write(
					f'scoreboard players operation {item.iid} craftlang += stack.{len(stack)} craftlang\r\n'
					f'execute if score {item.iid} craftlang matches 1 run scoreboard players set {item.iid} craftlang'
					' 0\r\n'
				)
			elif item.type == VarType.ENTITY and type == VarType.ENTITY:
				out.write(f'tag @e[tag={item.iid},tag=!stack.{len(stack)}] remove {item.iid}\r\n')
			else:
				raise Exception()  # TODO

		elif operator == '^=':
			item = _scope_get(scope, local)
			if item.type == VarType.BOOLEAN and type == VarType.BOOLEAN:
				out.write(
					f'scoreboard players operation {item.iid} craftlang += stack.{len(stack)} craftlang\r\n'
					f'execute if score {item.iid} craftlang matches 2 run scoreboard players set {item.iid} craftlang'
					' 0\r\n'
				)
			elif item.type == VarType.ENTITY and type == VarType.ENTITY:
				out.write(
					f'tag @e remove stack.{len(stack) + 1}\r\n'
					f'tag @e[tag={item.iid},tag=stack.{len(stack)}] add stack.{len(stack) + 1}\r\n'
					f'tag @e[tag=stack.{len(stack)}] add {id}\r\n'
					f'tag @e[tag=stack.{len(stack) + 1}] remove {id}\r\n'
				)
			else:
				raise Exception()  # TODO

		elif operator == '|=':
			item = _scope_get(scope, local)
			if item.type == VarType.BOOLEAN and type == VarType.BOOLEAN:
				out.write(
					f'scoreboard players operation {item.iid} craftlang += stack.{len(stack)} craftlang\r\n'
					f'execute if score {item.iid} craftlang matches 2 run scoreboard players set {item.iid} craftlang'
					' 1\r\n'
				)
			elif item.type == VarType.ENTITY and type == VarType.ENTITY:
				out.write(f'tag @e[tag=stack.{len(stack)}] add {item.iid}\r\n')
			else:
				raise Exception()  # TODO

		else:
			raise Exception()  # TODO

	elif isinstance(statement, ReturnStatement):
		out.write(f'# {statement}\r\n')

		compile_expr(file, statement.expr, out, scope, stack)
		if stack.pop() != ret_type:
			raise Exception()  # TODO

		out.write(f'scoreboard players operation {ret} craftlang = stack.{len(stack)} craftlang\r\n')

	elif isinstance(statement, IfStatement):
		out.write(f'# if {statement.condition}\r\n')

		compile_expr(file, statement.condition, out, scope, stack)
		if stack.pop() != VarType.BOOLEAN:
			raise Exception()  # TODO

		if len(statement.if_true) > 0:
			aux = next(aux_gen)
			out.write(f'execute if score stack.{len(stack)} craftlang matches 1 run function {aux.id}\r\n')

			with aux.out as o:
				for s in statement.if_true:
					compile_statement(file, s, o, aux_gen, scope, iid_gen, ret, ret_type)

		if len(statement.if_false) > 0:
			aux = next(aux_gen)
			out.write(f'execute if score stack.{len(stack)} craftlang matches 0 run function {aux.id}\r\n')

			with aux.out as o:
				for s in statement.if_false:
					compile_statement(file, s, o, aux_gen, scope, iid_gen, ret, ret_type)

	elif isinstance(statement, WhileStatement):
		out.write(f'# while {statement.condition}\r\n')

		compile_expr(file, statement.condition, out, scope, stack)
		if stack.pop() != VarType.BOOLEAN:
			raise Exception()  # TODO

		if len(statement.statements) > 0:
			aux = next(aux_gen)
			out.write(f'execute if score stack.{len(stack)} craftlang matches 1 run function {aux.id}\r\n')

			with aux.out as o:
				for s in statement.statements:
					compile_statement(file, s, o, aux_gen, scope, iid_gen, ret, ret_type)

				compile_expr(file, statement.condition, o, scope, stack)
				if stack.pop() != VarType.BOOLEAN:
					raise Exception()  # TODO

				o.write(f'execute if score stack.{len(stack)} craftlang matches 1 run function {aux.id}\r\n')

	elif isinstance(statement, DoWhileStatement):
		out.write('# do\r\n')

		if len(statement.statements) > 0:
			aux = next(aux_gen)
			out.write(f'function {aux.id}\r\n')

			with aux.out as o:
				for s in statement.statements:
					compile_statement(file, s, o, aux_gen, scope, iid_gen, ret, ret_type)

				compile_expr(file, statement.condition, o, scope, stack)
				if stack.pop() != VarType.BOOLEAN:
					raise Exception()  # TODO

				o.write(f'execute if score stack.{len(stack)} craftlang matches 1 run function {aux.id}\r\n')

	elif isinstance(statement, FuncCall):
		out.write(f'# {statement}\r\n')
		_compile_function_call(file, statement, out, scope, stack)

	else:
		raise Exception()  # TODO


def compile_expr(file: File, expr: Expr, out: TextIO, scope: Scope, stack: List[VarType]) -> None:
	if isinstance(expr, ParensExpr):
		compile_expr(file, expr.expr, out, scope, stack)

	elif isinstance(expr, LiteralExpr):
		type = expr.type
		literal = str(expr.token)

		if type == VarType.BOOLEAN:
			if literal == 'false':
				out.write(f'scoreboard players set stack.{len(stack)} craftlang 0\r\n')
			elif literal == 'true':
				out.write(f'scoreboard players set stack.{len(stack)} craftlang 1\r\n')
			else:
				raise Exception()  # TODO
			stack += [VarType.BOOLEAN]

		elif type == VarType.SCORE:
			out.write(f'scoreboard players set stack.{len(stack)} craftlang {literal}\r\n')
			stack += [VarType.SCORE]

		elif type == VarType.ENTITY:
			out.write(f'tag @e remove stack.{len(stack)}\r\n')
			if len(literal) > 0:
				out.write(f'tag {literal} add stack.{len(stack)}\r\n')
			stack += [VarType.ENTITY]

		else:
			raise Exception()  # TODO

	elif isinstance(expr, IdentifierExpr):
		local = str(expr.token)
		item = _scope_get(scope, local)

		if item is None:
			if local == 'false':
				out.write(f'scoreboard players set stack.{len(stack)} craftlang 0\r\n')
				stack += [VarType.BOOLEAN]
			elif local == 'true':
				out.write(f'scoreboard players set stack.{len(stack)} craftlang 1\r\n')
				stack += [VarType.BOOLEAN]
			else:
				raise Exception()  # TODO

		else:
			if item.type == VarType.BOOLEAN or item.type == VarType.SCORE:
				out.write(f'scoreboard players operation stack.{len(stack)} craftlang = {item.iid} craftlang\r\n')
				stack += [item.type]
			elif item.type == VarType.ENTITY:
				out.write(
					f'tag @e remove stack.{len(stack)}\r\n'
					f'tag @e[tag={item.iid}] add stack.{len(stack)}\r\n'
				)
				stack += [VarType.ENTITY]
			else:
				raise Exception()  # TODO

	elif isinstance(expr, UnaryExpr):
		operator = str(expr.operator)
		type = stack.pop()

		if operator == '!':
			if type == VarType.BOOLEAN:
				out.write(
					f'scoreboard players add stack.{len(stack)} craftlang 1\r\n'
					f'execute if score stack.{len(stack)} craftlang matches 2 run scoreboard players set'
					f' stack.{len(stack)} craftlang 0\r\n'
				)
			else:
				raise Exception()  # TODO

		elif operator == '+':
			if type == VarType.SCORE:
				pass
			else:
				raise Exception()  # TODO

		elif operator == '-':
			if type == VarType.SCORE:
				out.write(
					f'scoreboard players set stack.{len(stack)} craftlang -1\r\n'
					f'scoreboard players operation stack.{len(stack) - 1} craftlang *= stack.{len(stack)}'
					' craftlang\r\n'
				)
			else:
				raise Exception()  # TODO

		else:
			raise Exception()  # TODO

	elif isinstance(expr, BinaryExpr):
		compile_expr(file, expr.left, out, scope, stack)
		compile_expr(file, expr.right, out, scope, stack)
		right_type = stack.pop()
		operator = str(expr.operator)
		left_type = stack.pop()

		if operator == '*':
			if left_type == VarType.SCORE and right_type == VarType.SCORE:
				out.write(
					f'scoreboard players operation stack.{len(stack)} craftlang *= stack.{len(stack) + 1}'
					' craftlang\r\n'
				)
				stack += [VarType.SCORE]
			else:
				raise Exception()  # TODO

		elif operator == '/':
			if left_type == VarType.SCORE and right_type == VarType.SCORE:
				out.write(
					f'scoreboard players operation stack.{len(stack)} craftlang /= stack.{len(stack) + 1}'
					' craftlang\r\n'
				)
				stack += [VarType.SCORE]
			else:
				raise Exception()  # TODO

		elif operator == '%':
			if left_type == VarType.SCORE and right_type == VarType.SCORE:
				out.write(
					f'scoreboard players operation stack.{len(stack)} craftlang %= stack.{len(stack) + 1}'
					' craftlang\r\n'
				)
				stack += [VarType.SCORE]
			else:
				raise Exception()  # TODO

		elif operator == '+':
			if left_type == VarType.SCORE and right_type == VarType.SCORE:
				out.write(
					f'scoreboard players operation stack.{len(stack)} craftlang += stack.{len(stack) + 1}'
					' craftlang\r\n'
				)
				stack += [VarType.SCORE]
			else:
				raise Exception()  # TODO

		elif operator == '-':
			if left_type == VarType.SCORE and right_type == VarType.SCORE:
				out.write(
					f'scoreboard players operation stack.{len(stack)} craftlang -= stack.{len(stack) + 1}'
					' craftlang\r\n'
				)
				stack += [VarType.SCORE]
			elif left_type == VarType.ENTITY and right_type == VarType.ENTITY:
				out.write(f'tag @e[tag=stack.{len(stack) + 1}] remove stack.{len(stack)}\r\n')
				stack += [VarType.ENTITY]
			else:
				raise Exception()  # TODO

		elif operator == '<':
			if left_type == VarType.SCORE and right_type == VarType.SCORE:
				out.write(
					f'scoreboard players operation stack.{len(stack) + 2} craftlang = stack.{len(stack)} craftlang\r\n'
					f'scoreboard players set stack.{len(stack)} craftlang 0\r\n'
					f'execute if score stack.{len(stack) + 2} craftlang < stack.{len(stack) + 1} craftlang run'
					f' scoreboard players set stack.{len(stack)} craftlang 1\r\n'
				)
				stack += [VarType.BOOLEAN]
			elif left_type == VarType.ENTITY and right_type == VarType.ENTITY:
				out.write(
					f'scoreboard players set stack.{len(stack)} craftlang 0\r\n'
					f'execute if entity @e[tag=!stack.{len(stack)},tag=stack.{len(stack) + 1}] run scoreboard players'
					f' set stack.{len(stack)} craftlang 1\r\n'
					f'execute if entity @e[tag=stack.{len(stack)},tag=!stack.{len(stack) + 1}] run scoreboard players'
					f' set stack.{len(stack)} craftlang 0\r\n'
				)
				stack += [VarType.BOOLEAN]
			else:
				raise Exception()  # TODO

		elif operator == '>':
			if left_type == VarType.SCORE and right_type == VarType.SCORE:
				out.write(
					f'scoreboard players operation stack.{len(stack) + 2} craftlang = stack.{len(stack)} craftlang\r\n'
					f'scoreboard players set stack.{len(stack)} craftlang 0\r\n'
					f'execute if score stack.{len(stack) + 2} craftlang > stack.{len(stack) + 1} craftlang run'
					f' scoreboard players set stack.{len(stack)} craftlang 1\r\n'
				)
				stack += [VarType.BOOLEAN]
			elif left_type == VarType.ENTITY and right_type == VarType.ENTITY:
				out.write(
					f'scoreboard players set stack.{len(stack)} craftlang 0\r\n'
					f'execute if entity @e[tag=stack.{len(stack)},tag=!stack.{len(stack) + 1}] run scoreboard players'
					f' set stack.{len(stack)} craftlang 1\r\n'
					f'execute if entity @e[tag=!stack.{len(stack)},tag=stack.{len(stack) + 1}] run scoreboard players'
					f' set stack.{len(stack)} craftlang 0\r\n'
				)
				stack += [VarType.BOOLEAN]
			else:
				raise Exception()  # TODO

		elif operator == '<=':
			if left_type == VarType.SCORE and right_type == VarType.SCORE:
				out.write(
					f'scoreboard players operation stack.{len(stack) + 2} craftlang = stack.{len(stack)} craftlang\r\n'
					f'scoreboard players set stack.{len(stack)} craftlang 0\r\n'
					f'execute if score stack.{len(stack) + 2} craftlang <= stack.{len(stack) + 1} craftlang run'
					f' scoreboard players set stack.{len(stack)} craftlang 1\r\n'
				)
				stack += [VarType.BOOLEAN]
			elif left_type == VarType.ENTITY and right_type == VarType.ENTITY:
				out.write(
					f'scoreboard players set stack.{len(stack)} craftlang 1\r\n'
					f'execute if entity @e[tag=stack.{len(stack)},tag=!stack.{len(stack) + 1}] run scoreboard players'
					f' set stack.{len(stack)} craftlang 0\r\n'
				)
				stack += [VarType.BOOLEAN]
			else:
				raise Exception()  # TODO

		elif operator == '>=':
			if left_type == VarType.SCORE and right_type == VarType.SCORE:
				out.write(
					f'scoreboard players operation stack.{len(stack) + 2} craftlang = stack.{len(stack)} craftlang\r\n'
					f'scoreboard players set stack.{len(stack)} craftlang 0\r\n'
					f'execute if score stack.{len(stack) + 2} craftlang >= stack.{len(stack) + 1} craftlang run'
					f' scoreboard players set stack.{len(stack)} craftlang 1\r\n'
				)
				stack += [VarType.BOOLEAN]
			elif left_type == VarType.ENTITY and right_type == VarType.ENTITY:
				out.write(
					f'scoreboard players set stack.{len(stack)} craftlang 1\r\n'
					f'execute if entity @e[tag=!stack.{len(stack)},tag=stack.{len(stack) + 1}] run scoreboard players'
					f' set stack.{len(stack)} craftlang 0\r\n'
				)
				stack += [VarType.BOOLEAN]
			else:
				raise Exception()  # TODO

		elif operator == '==':
			if (
					(left_type == VarType.SCORE and right_type == VarType.SCORE) or
					(left_type == VarType.BOOLEAN and right_type == VarType.BOOLEAN)
			):
				out.write(
					f'scoreboard players operation stack.{len(stack) + 2} craftlang = stack.{len(stack)} craftlang\r\n'
					f'scoreboard players set stack.{len(stack)} craftlang 0\r\n'
					f'execute if score stack.{len(stack) + 2} craftlang = stack.{len(stack) + 1} craftlang run'
					f' scoreboard players set stack.{len(stack)} craftlang 1\r\n'
				)
				stack += [VarType.BOOLEAN]
			elif left_type == VarType.ENTITY and right_type == VarType.ENTITY:
				out.write(
					f'scoreboard players set stack.{len(stack)} craftlang 1\r\n'
					f'execute if entity @e[tag=stack.{len(stack)},tag=!stack.{len(stack) + 1}] run scoreboard players'
					f' set stack.{len(stack)} craftlang 0\r\n'
					f'execute if entity @e[tag=!stack.{len(stack)},tag=stack.{len(stack) + 1}] run scoreboard players'
					f' set stack.{len(stack)} craftlang 0\r\n'
				)
				stack += [VarType.BOOLEAN]
			else:
				raise Exception()  # TODO

		elif operator == '&':
			if left_type == VarType.BOOLEAN and right_type == VarType.BOOLEAN:
				out.write(
					f'scoreboard players operation stack.{len(stack)} craftlang += stack.{len(stack) + 1}'
					' craftlang\r\n'
					f'execute if score stack.{len(stack)} craftlang matches 1 run scoreboard players set'
					f' stack.{len(stack)} craftlang 0\r\n'
				)
				stack += [VarType.BOOLEAN]
			elif left_type == VarType.ENTITY and right_type == VarType.ENTITY:
				out.write(
					f'tag @e[tag=stack.{len(stack)},tag=!stack.{len(stack) + 1}] remove stack.{len(stack) + 1}\r\n'
				)
				stack += [VarType.ENTITY]
			else:
				raise Exception()  # TODO

		elif operator == '^':
			if left_type == VarType.BOOLEAN and right_type == VarType.BOOLEAN:
				out.write(
					f'scoreboard players operation stack.{len(stack)} craftlang += stack.{len(stack) + 1}'
					' craftlang\r\n'
					f'execute if score stack.{len(stack)} craftlang matches 2 run scoreboard players set'
					f' stack.{len(stack)} craftlang 0\r\n'
				)
				stack += [VarType.BOOLEAN]
			elif left_type == VarType.ENTITY and right_type == VarType.ENTITY:
				out.write(
					f'tag @e remove stack.{len(stack) + 2}\r\n'
					f'tag @e[tag=stack.{len(stack)},tag=stack.{len(stack) + 1}] add stack.{len(stack) + 2}\r\n'
					f'tag @e[tag=stack.{len(stack) + 1}] add stack.{len(stack)}\r\n'
					f'tag @e[tag=stack.{len(stack) + 2}] remove stack.{len(stack)}\r\n'
				)
				stack += [VarType.ENTITY]
			else:
				raise Exception()  # TODO

		elif operator == '|':
			if left_type == VarType.BOOLEAN and right_type == VarType.BOOLEAN:
				out.write(
					f'scoreboard players operation stack.{len(stack)} craftlang += stack.{len(stack) + 1}'
					' craftlang\r\n'
					f'execute if score stack.{len(stack)} craftlang matches 2 run scoreboard players set'
					f' stack.{len(stack)} craftlang 1\r\n'
				)
				stack += [VarType.BOOLEAN]
			elif left_type == VarType.ENTITY and right_type == VarType.ENTITY:
				out.write(f'tag @e[tag=stack.{len(stack) + 1}] add stack.{len(stack)}\r\n')
				stack += [VarType.ENTITY]
			else:
				raise Exception()  # TODO

		else:
			raise Exception()  # TODO

	elif isinstance(expr, FuncCall):
		_compile_function_call(file, expr, out, scope, stack)

	else:
		raise Exception()  # TODO


def _compile_function_call(file: File, func_call: FuncCall, out: TextIO, scope: Scope, stack: List[VarType]) -> None:
	func_def = next(filter(lambda f: str(func_call.identifier) == str(f.identifier), file.func_defs))  # TODO
	if len(func_call.args) != len(func_def.params):
		raise Exception()  # TODO

	nc = file.namespace_decl.components

	for i, arg in enumerate(func_call.args):
		if arg.by_ref:
			item = _scope_get(scope, str(arg.expr))
			id = item.iid
			type = item.type
		else:
			compile_expr(file, arg.expr, out, scope, stack)
			id = f'stack.{len(stack)}'
			type = stack.pop()

		if type != VarType.from_str(str(func_def.params[i].type)):
			raise Exception()  # TODO

		if type == VarType.BOOLEAN or type == VarType.SCORE:
			out.write(
				f'scoreboard players operation args.{".".join(map(lambda c: _asciify(str(c)), nc))}'
				f'.{_asciify(str(func_call.identifier))}.{i} craftlang ='
				f' {id} craftlang\r\n'
			)
		elif type == VarType.ENTITY:
			t = f'args.{".".join(map(lambda c: _asciify(str(c)), nc))}.{_asciify(str(func_call.identifier))}.{i}'
			out.write(
				f'tag @e remove {t}\r\n'
				f'tag @e[tag={id}] add {t}\r\n'
			)
		else:
			raise Exception()  # TODO

	out.write(f'''function {nc[0]}:{"/".join(
		chain(map(lambda c: _asciify(str(c)), islice(nc, 1, None)), [str(func_call.identifier)])
	)}\r\n''')

	for i, arg in enumerate(func_call.args):
		if arg.by_ref:
			item = _scope_get(scope, str(arg.expr))
			type = item.type
			id = item.iid

			if type == VarType.BOOLEAN or type == VarType.SCORE:
				out.write(
					f'scoreboard players operation {id} craftlang ='
					f' args.{".".join(map(lambda c: _asciify(str(c)), nc))}.{_asciify(str(func_call.identifier))}.{i}'
					' craftlang\r\n'
				)
			elif type == VarType.ENTITY:
				out.write(
					f'tag @e remove {id}\r\n'
					f'tag @e[tag=args.{".".join(map(lambda c: _asciify(str(c)), nc))}'
					f'.{_asciify(str(func_call.identifier))}.{i}] add {id}\r\n'
				)
			else:
				raise Exception()  # TODO


def _scope_get(scope: Scope, id: str) -> Scope.Item:
	item = scope.get(id)
	if item is None:
		raise Exception()  # TODO
	else:
		return item


def _asciify(s: str) -> str:
	return str(normalize('NFKD', s).encode('ASCII', 'ignore'), 'UTF-8')


@dataclass
class Auxiliary:
	id: str
	out: TextIO
