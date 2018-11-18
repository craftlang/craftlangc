__author__ = 'Matteo Morena'
__all__ = (
	'File', 'NamespaceDecl', 'FuncDef', 'Statement', 'NopStatement', 'CommandStatement', 'AssignStatement',
	'SwapStatement', 'ReturnStatement', 'IfStatement', 'WhileStatement', 'DoWhileStatement', 'Expr', 'ParensExpr',
	'UnaryExpr', 'BinaryExpr', 'IdentifierExpr', 'LiteralExpr', 'FuncCall', 'Token'
)

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import List, Union

from craftlangc.enums import VarType
from craftlangc.walker import Walker


@dataclass
class File:
	namespace_decl: 'NamespaceDecl'
	func_defs: List['FuncDef']

	def __str__(self) -> str:
		result = str(self.namespace_decl)
		if len(self.func_defs) > 0:
			result += '\r\n\r\n' + '\r\n\r\n'.join(map(str, self.func_defs))
		return result


@dataclass
class NamespaceDecl:
	components: List['Token']

	def __str__(self) -> str:
		return f'namespace {".".join(map(str, self.components))}'


@dataclass
class FuncDef:
	identifier: 'Token'
	tags: List['Token']
	params: List['Param']
	return_type: 'Token'
	statements: List['Statement']

	def __str__(self) -> str:
		result = f'{self.identifier}({", ".join(map(str, self.params))}): {self.return_type}'
		if len(self.statements) > 0:
			result += '\r\n' + '\r\n'.join(map(
				lambda s: s._to_str(1), self.statements
			))
		return result

	@dataclass
	class Param:
		identifier: 'Token'
		type: 'Token'

		def __str__(self) -> str:
			return f'{self.identifier}: {self.type}'


class Statement(ABC):
	@abstractmethod
	def _to_str(self, indent: int) -> str:
		raise NotImplementedError()


@dataclass
class NopStatement(Statement):
	def _to_str(self, indent: int) -> str:
		return '\t' * indent + 'nop'

	def __str__(self) -> str:
		return self._to_str(0)


@dataclass
class CommandStatement(Statement):
	components: List[Union['Token', 'Arg']]

	@classmethod
	def _component_to_str(cls, component: Union['Token', 'Arg']) -> str:
		if isinstance(component, Token):
			return str(component)
		elif isinstance(component, Arg):
			return f'$({component})'
		else:
			raise Exception()  # TODO

	def _to_str(self, indent: int) -> str:
		return '\t' * indent + f'/{"".join(map(lambda c: self._component_to_str(c), self.components))}'

	def __str__(self) -> str:
		return self._to_str(0)


@dataclass
class AssignStatement(Statement):
	identifier: 'Token'
	operator: 'Token'
	expr: 'Expr'

	def _to_str(self, indent: int) -> str:
		return '\t' * indent + f'{self.identifier} {self.operator} {self.expr}'

	def __str__(self) -> str:
		return self._to_str(0)


@dataclass
class SwapStatement(Statement):
	left: 'Token'
	right: 'Token'

	def _to_str(self, indent: int) -> str:
		return '\t' * indent + f'{self.left} >< {self.right}'

	def __str__(self) -> str:
		return self._to_str(0)


@dataclass
class ReturnStatement(Statement):
	expr: 'Expr'

	def _to_str(self, indent: int) -> str:
		return f'return {self.expr}'

	def __str__(self) -> str:
		return self._to_str(0)


@dataclass
class IfStatement(Statement):
	condition: 'Expr'
	if_true: List[Statement]
	if_false: List[Statement]

	def _to_str(self, indent: int) -> str:
		result = '\t' * indent + f'if {self.condition}'
		if len(self.if_true) > 0:
			result += '\r\n' + '\r\n'.join(map(lambda o: o._to_str(indent + 1), self.if_true))
		if len(self.if_false) > 0:
			result += '\r\nelse\r\n' + '\r\n'.join(map(lambda o: o._to_str(indent + 1), self.if_false))
		return result

	def __str__(self) -> str:
		return self._to_str(0)


@dataclass
class WhileStatement(Statement):
	condition: 'Expr'
	statements: List[Statement]

	def _to_str(self, indent: int) -> str:
		result = '\t' * indent + f'while {self.condition}'
		if len(self.statements) > 0:
			result += '\r\n' + '\r\n'.join(map(lambda o: o._to_str(indent + 1), self.statements))
		return result

	def __str__(self) -> str:
		return self._to_str(0)


@dataclass
class DoWhileStatement(Statement):
	statements: List[Statement]
	condition: 'Expr'

	def _to_str(self, indent: int) -> str:
		result = '\t' * indent + f'do'
		if len(self.statements) > 0:
			result += '\r\n' + '\r\n'.join(map(lambda o: o._to_str(indent + 1), self.statements))
		return result + f'\r\nwhile {self.condition}'

	def __str__(self) -> str:
		return self._to_str(0)


@dataclass
class Expr:
	pass


@dataclass
class ParensExpr(Expr):
	expr: Expr

	def __str__(self) -> str:
		return f'({self.expr})'


@dataclass
class UnaryExpr(Expr):
	operator: 'Token'
	expr: Expr

	def __str__(self) -> str:
		return f'{self.operator} {self.expr}'


@dataclass
class BinaryExpr(Expr):
	left: Expr
	operator: 'Token'
	right: Expr

	def __str__(self) -> str:
		return f'{self.left} {self.operator} {self.right}'


@dataclass
class IdentifierExpr(Expr):
	token: 'Token'

	def __str__(self) -> str:
		return str(self.token)


@dataclass
class LiteralExpr(Expr):
	token: 'Token'
	type: VarType

	def __str__(self) -> str:
		if self.type == VarType.ENTITY:
			return f'<{self.token}>'
		else:
			return str(self.token)


@dataclass
class FuncCall(Statement, Expr):
	identifier: 'Token'
	args: List['Arg']

	def _to_str(self, indent: int) -> str:
		return '\t' * indent + f'{self.identifier}({", ".join(map(str, self.args))})'

	def __str__(self) -> str:
		return f'{self.identifier}({", ".join(map(str, self.args))})'


@dataclass
class Arg:
	expr: 'Expr'
	by_ref: bool

	def __str__(self) -> str:
		return f'{"ref " if self.by_ref else ""}{self.expr}'


@dataclass
class Token:
	walker: Walker
	pos: int
	len: int

	@property
	def line(self) -> int:
		pos = self.walker.pos
		self.walker.pos = self.pos
		result = self.walker.line
		self.walker.pos = pos
		return result

	@property
	def column(self) -> int:
		pos = self.walker.pos
		self.walker.pos = self.pos
		result = self.walker.column
		self.walker.pos = pos
		return result

	def __len__(self) -> int:
		return self.len

	def __str__(self) -> str:
		return self.walker.source[self.pos:(self.pos + len(self))]
