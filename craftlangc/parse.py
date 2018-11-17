'''
Provides functions for parsing the Craftlang programming language

All functions follow the following conventions:
 * each function parses a single feature of the Craftlang language. Some features (like functions and expressions)
   are inherently more complex and need bigger methods and/or the use of helper functions. These conventions apply
   to helper methods as well;
 * each function starts parsing at the current cursor position and advances the cursor only by the minimum amount
   required to do the parsing;
 * if the cursor is advanced beyond the parsed feature in order to look ahead, the cursor has to be restored at the
   end of the feature before the method returns.
'''

__author__ = 'Matteo Morena'
__all__ = ('parse_file', 'parse_statement', 'parse_block', 'parse_expr', 'ParseError')

from typing import List

from craftlangc.character import is_digit, is_identifier_continue, is_identifier_start, is_newline, is_whitespace
from craftlangc.cst import AssignStatement, BinaryExpr, CommandStatement, DoWhileStatement, Expr, File, FuncCall, \
	FuncDef, IdentifierExpr, IfStatement, LiteralExpr, NamespaceDecl, NopStatement, ParensExpr, ReturnStatement, \
	Statement, SwapStatement, Token, UnaryExpr, WhileStatement
from craftlangc.enums import VarType
from craftlangc.walker import Walker


def parse_file(walker: Walker) -> File:
	if _parse_indent(walker) > 0:
		raise ParseError('Unexpected indent')

	namespace = _parse_namespace_decl(walker)
	func_defs: List[FuncDef] = []

	while True:
		indent = _parse_indent(walker)

		if walker.ahead() == '':
			break

		if indent > 0:
			raise ParseError('Unexpected indent')

		pos = walker.pos
		func_def = _parse_func_def(walker)
		if str(func_def.identifier) in map(lambda f: str(f.identifier), func_defs):
			walker.pos = pos
			raise ParseError(f"Function '{func_def.identifier}' already defined")

		func_defs += [func_def]

	return File(namespace, func_defs)


def _parse_namespace_decl(walker: Walker) -> NamespaceDecl:
	if walker.match('namespace') is None:
		raise ParseError('Expected namespace declaration')

	if len(walker.match(is_whitespace) or '') == 0:
		raise ParseError('Expected whitespace')

	pos = walker.pos
	components: List[Token] = []

	while True:
		component = _parse_identifier(walker)
		if len(component) == 0:
			walker.pos = pos
			raise ParseError('Illegal namespace identifier')
		components += [component]

		pos = walker.pos
		walker.match(is_whitespace)
		if walker.advance() == '.':
			walker.match(is_whitespace)
		else:
			walker.pos = pos
			break

	if len(components) == 0:
		raise ParseError('Illegal namespace identifier')

	return NamespaceDecl(components)


def _parse_func_def(walker: Walker) -> FuncDef:
	identifier = _parse_identifier(walker)
	if len(identifier) == 0:
		raise ParseError('Illegal function identifier')

	walker.match(is_whitespace)
	if walker.match('(') is None:
		raise ParseError("Expected '('")

	walker.match(is_whitespace)
	parameters: List[FuncDef.Param] = []

	while walker.match(')') != ')':
		parameter_identifier = _parse_identifier(walker)
		if len(parameter_identifier) == 0:
			raise ParseError('Illegal function parameter identifier')

		walker.match(is_whitespace)
		if walker.match(':') is None:
			raise ParseError("Expected ':'")

		walker.match(is_whitespace)
		type = _parse_identifier(walker)
		if len(type) == 0:
			raise ParseError('Illegal function parameter type')

		parameters += [FuncDef.Param(parameter_identifier, type)]

		ahead = walker.ahead()
		if ahead == ',':
			walker.advance()
			walker.match(is_whitespace)
			if walker.ahead() == ')':
				raise ParseError("Unexpected ')'")

	walker.match(is_whitespace)
	if walker.advance() != ':':
		walker.retreat()
		raise ParseError("Expected ':'")

	walker.match(is_whitespace)
	return_type = _parse_identifier(walker)
	if len(return_type) == 0:
		raise ParseError('Illegal function return type')

	walker.match(is_whitespace)
	if not is_newline(walker.ahead()):
		raise ParseError('Expected newline')

	return FuncDef(identifier, [], parameters, return_type, parse_block(walker, 0))


def parse_statement(walker: Walker, current_indent: int) -> Statement:
	# If '/' is ahead, we know we have a command
	if walker.match('/') is not None:
		pos = walker.pos
		walker.match(lambda c: not is_newline(c) and c != '')
		return CommandStatement(Token(walker, pos, walker.pos - pos))

	# Otherwise, all other statements start with an identifier (which might be a keyword)
	initial_pos = walker.pos
	identifier = _parse_identifier(walker)
	if len(identifier) == 0:
		raise ParseError('Invalid statement')

	pos = walker.pos
	identifier_lexeme = str(identifier)
	walker.match(is_whitespace)
	ahead2 = walker.ahead(2)
	ahead = ahead2[:1]

	# If we got a '=' after the identifier, we have an assignment
	if ahead == '=':
		pos = walker.pos
		walker.advance()
		operator = Token(walker, pos, walker.pos - pos)
		walker.match(is_whitespace)
		return AssignStatement(identifier, operator, parse_expr(walker))

	# If we got '*=', '/=', '%=', '+=', '-=', '&=', '^=' or '|=' after the identifier, we have an assignment
	if ahead2 in {'*=', '/=', '%=', '+=', '-=', '&=', '^=', '|='}:
		pos = walker.pos
		walker.advance(2)
		operator = Token(walker, pos, walker.pos - pos)
		walker.match(is_whitespace)
		return AssignStatement(identifier, operator, parse_expr(walker))

	# If we got '><' after the identifier, we have a swap statement
	if ahead2 == '><':
		walker.advance(2)
		walker.match(is_whitespace)
		second_identifier = _parse_identifier(walker)
		if len(second_identifier) == 0:
			raise ParseError('Illegal second identifier for swap statement')
		return SwapStatement(identifier, second_identifier)

	# If the identifier is 'nop' and we have no other characters on the same line, we deduce that the identifier is in
	# fact a 'nop' keyword, thus giving us a nop statement
	if identifier_lexeme == 'nop':
		ahead2 = walker.ahead()
		if is_newline(ahead2) or ahead2 == '':
			return NopStatement()

	# If we got a '(' after the identifier, we probably have a function call.
	# There's an ambiguity if identifier is 'if' or 'while': do we have a function call or an if/while statement?
	if ahead == '(':
		arguments = _parse_args(walker)

		# After we found the list of arguments for the probable function call, we deal with the ambiguity mentioned
		# before. If the identifier is 'if' or 'while' we resolve the ambiguity in the following manner:
		#  * 0 arguments: definitively a function call;
		#  * 1 argument, passed by value: maybe an if/while statement (*);
		#  * 1 argument, passed by reference: definitively a function call;
		#  * 2 arguments or more: definitively a function call;
		#
		# We start by assuming that we have a function call. Once condition (*) is verified, we check if a newline and
		# an indent is ahead. If this new indent is greater than our current indent, we are sure that we have an
		# if/while statement; otherwise, we have a function call.

		is_call_statement = True

		if identifier_lexeme in {'if', 'while'} and len(arguments) == 1 and not arguments[0].by_ref:
			pos2 = walker.pos
			walker.match(is_whitespace)
			ahead3 = walker.ahead()

			if ahead3 == '':
				pass
			elif not is_newline(ahead3):
				is_call_statement = False
			else:
				walker.advance()
				if _parse_indent(walker) > current_indent:
					is_call_statement = False

			walker.pos = pos2

		if is_call_statement:
			return FuncCall(identifier, arguments)

	# None of the conditions above have been fulfilled. At this point, if/while/do-while statements are still an option.
	# However, since we moved our walker around in the code above, we reset its position to the position after the
	# identifier.
	walker.pos = pos

	# If the identifier is 'while', we have a return statement
	if identifier_lexeme == 'return':
		if len(walker.match(is_whitespace) or '') == 0 and walker.ahead() != '(':
			raise ParseError('Expected return expression')

		return ReturnStatement(parse_expr(walker))

	# If the identifier is 'if', we have an if statement
	if identifier_lexeme == 'if':
		if len(walker.match(is_whitespace) or '') == 0 and walker.ahead() != '(':
			raise ParseError('Expected if condition')

		condition = parse_expr(walker)
		walker.match(is_whitespace)

		if walker.match(is_newline) is None:
			raise ParseError('Expected newline after if condition')

		if_true = parse_block(walker, current_indent)

		# We advance the position of our walker, looking for a lonely 'else' on the next line. If there is this 'else',
		# we also parse the else part of the if statement; otherwise, we reset the position of our walker to where it
		# was before.
		if_false = None
		pos = walker.pos
		walker.match(is_whitespace)
		if (
				walker.match(is_newline) is not None and _parse_indent(walker) == current_indent and
				walker.match('else') is not None
		):
			walker.match(is_whitespace)
			if walker.match(is_newline) is not None:
				if_false = parse_block(walker, current_indent)
		if if_false is None:
			if_false = []
			walker.pos = pos

		return IfStatement(condition, if_true, if_false)

	# If the identifier is 'while', we have a while statement
	if identifier_lexeme == 'while':
		if len(walker.match(is_whitespace) or '') == 0 and walker.ahead() != '(':
			raise ParseError('Expected while condition')

		condition = parse_expr(walker)
		walker.match(is_whitespace)

		if walker.match(is_newline) is None:
			raise ParseError('Expected newline after while condition')

		return WhileStatement(condition, parse_block(walker, current_indent))

	# If the identifier is 'do', we have a do-while statement
	if identifier_lexeme == 'do':
		walker.match(is_whitespace)
		if walker.match(is_newline) is None:
			raise ParseError("Expected newline after 'do'")

		block = parse_block(walker, current_indent)
		pos = walker.pos

		if _parse_indent(walker) == current_indent and str(_parse_identifier(walker)) == 'while':
			ahead2 = walker.ahead()
			if not is_whitespace(ahead2) and ahead2 != '(':
				raise ParseError('Expected condition for do-while statement')
			walker.match(is_whitespace)
			return DoWhileStatement(block, parse_expr(walker))
		else:
			walker.pos = pos
			raise ParseError('Expected condition for do-while statement')

	# We give up: none of the conditions above have been fulfilled.
	# We reset the walker position to where it was before this method got called and we raise an error.
	walker.pos = initial_pos
	raise ParseError('Illegal statement')


def parse_block(walker: Walker, current_indent: int) -> List[Statement]:
	new_indent = _parse_indent(walker)
	if new_indent <= current_indent:
		raise ParseError('Expected indent')

	block = [parse_statement(walker, new_indent)]

	while True:
		pos = walker.pos
		walker.match(is_whitespace)

		if walker.ahead() == '':
			walker.pos = pos
			break

		if not walker.match(is_newline):
			raise ParseError('Expected newline')

		continue_indent = _parse_indent(walker)

		if continue_indent == new_indent:
			block += [parse_statement(walker, new_indent)]
		elif continue_indent <= current_indent:
			walker.pos = pos
			break
		else:
			raise ParseError('Invalid indentation level')

	return block


def parse_expr(walker: Walker) -> Expr:
	return _parse_or_expr(walker)


def _parse_or_expr(walker: Walker) -> Expr:
	expr = _parse_xor_expr(walker)

	while True:
		pos = walker.pos
		walker.match(is_whitespace)

		if walker.ahead() != '|':
			walker.pos = pos
			break

		pos = walker.pos
		walker.advance()
		operator = Token(walker, pos, walker.pos - pos)

		walker.match(is_whitespace)
		expr = BinaryExpr(expr, operator, _parse_xor_expr(walker))

	return expr


def _parse_xor_expr(walker: Walker) -> Expr:
	expr = _parse_and_expr(walker)

	while True:
		pos = walker.pos
		walker.match(is_whitespace)

		if walker.ahead() != '^':
			walker.pos = pos
			break

		pos = walker.pos
		walker.advance()
		operator = Token(walker, pos, walker.pos - pos)

		walker.match(is_whitespace)
		expr = BinaryExpr(expr, operator, _parse_and_expr(walker))

	return expr


def _parse_and_expr(walker: Walker) -> Expr:
	expr = _parse_equality_expr(walker)

	while True:
		pos = walker.pos
		walker.match(is_whitespace)

		if walker.ahead() != '&':
			walker.pos = pos
			break

		pos = walker.pos
		walker.advance()
		operator = Token(walker, pos, walker.pos - pos)

		walker.match(is_whitespace)
		expr = BinaryExpr(expr, operator, _parse_equality_expr(walker))

	return expr


def _parse_equality_expr(walker: Walker) -> Expr:
	expr = _parse_relational_expr(walker)

	while True:
		pos = walker.pos
		walker.match(is_whitespace)

		if walker.ahead(2) not in {'==', '!='}:
			walker.pos = pos
			break

		pos = walker.pos
		walker.advance(2)
		operator = Token(walker, pos, walker.pos - pos)

		walker.match(is_whitespace)
		expr = BinaryExpr(expr, operator, _parse_relational_expr(walker))

	return expr


def _parse_relational_expr(walker: Walker) -> Expr:
	expr = _parse_additive_expr(walker)

	while True:
		pos = walker.pos
		walker.match(is_whitespace)

		if walker.ahead() not in {'<', '>'}:
			walker.pos = pos
			break

		pos = walker.pos
		walker.advance()
		if walker.ahead() == '=':
			walker.advance()
		operator = Token(walker, pos, walker.pos - pos)

		walker.match(is_whitespace)
		expr = BinaryExpr(expr, operator, _parse_additive_expr(walker))

	return expr


def _parse_additive_expr(walker: Walker) -> Expr:
	expr = _parse_multiplicative_expr(walker)

	while True:
		pos = walker.pos
		walker.match(is_whitespace)

		if walker.ahead() not in {'+', '-'}:
			walker.pos = pos
			break

		pos = walker.pos
		walker.advance()
		operator = Token(walker, pos, walker.pos - pos)

		walker.match(is_whitespace)
		expr = BinaryExpr(expr, operator, _parse_multiplicative_expr(walker))

	return expr


def _parse_multiplicative_expr(walker: Walker) -> Expr:
	expr = _parse_primary_expr(walker)

	while True:
		pos = walker.pos
		walker.match(is_whitespace)

		if walker.ahead() not in {'*', '/', '%'}:
			walker.pos = pos
			break

		pos = walker.pos
		walker.advance()
		operator = Token(walker, pos, walker.pos - pos)

		walker.match(is_whitespace)
		expr = BinaryExpr(expr, operator, _parse_primary_expr(walker))

	return expr


def _parse_primary_expr(walker: Walker) -> Expr:
	ahead2 = walker.ahead(2)
	ahead = ahead2[:1]
	ahead2 = ahead2[1:2]

	# If we got a '!', we have an unary expression.
	# If we got a '+' or a '-', we could have a score literal or an unary expression; since score literals are just
	# a (optional) '+' or '-' followed by one or more digit characters with no spaces in between, we can easily
	# check which case we are parsing by looking ahead one character further.
	if ahead == '!' or (ahead in {'+', '-'} and not is_digit(ahead2)):
		pos = walker.pos
		walker.advance()
		operator = Token(walker, pos, walker.pos - pos)
		walker.match(is_whitespace)
		return UnaryExpr(operator, parse_expr(walker))

	# If we got a '(', we have a parenthesised expression
	if ahead == '(':
		walker.advance()
		walker.match(is_whitespace)
		result = parse_expr(walker)
		walker.match(is_whitespace)
		if walker.advance() != ')':
			walker.retreat()
			raise ParseError('Unbalanced parenthesis')
		return ParensExpr(result)

	# If we got an integer, we have a literal score expression
	if is_digit(ahead) or (ahead in {'+', '-'} and is_digit(ahead2)):
		return LiteralExpr(_parse_integer(walker), VarType.SCORE)

	# If we got a '<', we have a literal entity expression
	if ahead == '<':
		walker.advance()
		pos = walker.pos
		while True:
			ahead = walker.ahead()
			if ahead == '>':
				token = Token(walker, pos, walker.pos - pos)
				walker.advance()
				break
			if is_newline(ahead):
				raise ParseError('Illegal newline')
			if ahead == '':
				raise ParseError('Unexpected EOF')
			walker.advance()
		return LiteralExpr(token, VarType.ENTITY)

	# None of the conditions above have been fulfilled. At this point, we can still have an identifier or a function
	# call.

	identifier = _parse_identifier(walker)
	if len(identifier) == 0:
		raise ParseError('Invalid expression')

	pos = walker.pos
	walker.match(is_whitespace)

	if walker.ahead() == '(':
		return _parse_func_call(walker)
	else:
		walker.pos = pos
		return IdentifierExpr(identifier)


def _parse_func_call(walker: Walker) -> FuncCall:
	identifier = _parse_identifier(walker)
	if len(identifier) == 0:
		raise ParseError('Invalid function name')

	walker.match(is_whitespace)

	return FuncCall(identifier, _parse_args(walker))


def _parse_args(walker: Walker) -> List[FuncCall.Arg]:
	if walker.match('(') is None:
		raise ParseError("Expected '('")

	args: List[FuncCall.Arg] = []

	while True:
		walker.match(is_whitespace)

		if walker.match(')') is not None:
			break
		elif walker.match(',') is not None and len(args) > 0:
			walker.match(is_whitespace)

		if walker.match('ref') is not None and len(walker.match(is_whitespace) or '') > 0:
			pos = walker.pos
			if len(_parse_identifier(walker)) == 0:
				raise ParseError('Illegal identifier')
			args += [FuncCall.Arg(IdentifierExpr(Token(walker, pos, walker.pos - pos)), True)]
		else:
			args += [FuncCall.Arg(parse_expr(walker), False)]

	return args


def _parse_indent(walker: Walker) -> int:
	while True:
		walker.match(is_newline)

		indent = 0
		while is_whitespace(walker.ahead()):
			if walker.advance() == '\t':
				indent = (indent + 4) // 4 * 4
			else:
				indent += 1

		if not is_newline(walker.ahead()):
			break

	return indent


def _parse_identifier(walker: Walker) -> Token:
	pos = walker.pos
	walker.match(lambda offset, c: is_identifier_start(c) if offset == 0 else is_identifier_continue(c))
	return Token(walker, pos, walker.pos - pos)


def _parse_integer(walker: Walker) -> Token:
	pos = walker.pos
	walker.match(lambda offset, c: (is_digit(c) or c in {'+', '-'}) if offset == 0 else is_digit(c))
	token = Token(walker, pos, walker.pos - pos)
	try:
		if int(str(token)) not in range(-(2 ** 31), 2 ** 31):
			walker.pos = pos
			raise ParseError('Integer out of range')
	except ValueError:
		walker.pos = pos
		raise ParseError('Invalid integer value')
	return token


class ParseError(Exception):
	pass