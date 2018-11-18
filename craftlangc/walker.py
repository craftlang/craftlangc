__author__ = 'Matteo Morena'
__all__ = ('Walker',)

from inspect import getfullargspec
from typing import Callable, Optional, Union, overload

from craftlangc.character import is_newline


class Walker:
	'''
	Permits the traversal of a character sequence
	'''

	__slots__ = frozenset({'_source', '_pos', '_line', '_column'})

	_source: str
	_pos: int
	_line: int
	_column: int

	def __init__(self, source: str) -> None:
		self._source = source
		self._pos = 0
		self._line = 0
		self._column = 0

	@property
	def source(self) -> str:
		'''
		Returns the character sequence being traversed
		'''

		return self._source

	@property
	def pos(self) -> int:
		'''
		Returns the current 0-indexed cursor position
		'''

		return self._pos

	@pos.setter
	def pos(self, pos: int) -> None:
		'''
		Sets the 0-indexed cursor position
		'''

		while self._pos < pos:
			character = self.source[self._pos:(self._pos + 1)]

			if character == '':
				break

			if character.isprintable():
				self._column += 1

			if is_newline(character) and character + self.source[(self._pos + 1):(self._pos + 2)] != '\r\n':
				self._line += 1
				self._column = 0

			self._pos += 1

		while self._pos > pos:
			character = self.source[(self._pos - 1):self._pos]

			if character == '':
				break

			if character.isprintable():
				self._column -= 1

			if is_newline(character) and character + self.source[self._pos:(self._pos + 1)] != '\r\n':
				self._line -= 1
				self._column = -1

			self._pos -= 1

		if self._column < 0:
			pos = self._pos
			self._column = 0

			while True:
				character = self.source[max(0, pos - 1):pos]

				if (is_newline(character) and character + self.source[pos:(pos + 1)] != '\r\n') or character == '':
					break

				pos -= 1
				if character.isprintable():
					self._column += 1

	@property
	def line(self) -> int:
		'''
		Returns the 0-indexed line number at the current cursor position
		'''

		return self._line

	@property
	def column(self) -> int:
		'''
		Returns the 0-indexed column number at the current cursor position
		'''

		return self._column

	def ahead(self, count: int = 1) -> str:
		'''
		Starting at the current cursor position, looks ahead ``count`` characters and returns them as result

		The length of the returned string might be shorter than ``count`` characters if an EOF is encountered.  If
		``count`` is negative, calling this method is equivalent to calling ``behind(-count)``.  This method has no
		side effects.
		'''

		if count < 0:
			return self.behind(-count)

		return self.source[self.pos:(self.pos + count)]

	def behind(self, count: int = 1) -> str:
		'''
		Starting at the current cursor position, looks behind ``count`` characters and returns them as result

		The length of the returned string might be shorter than ``count`` characters if an EOF is encountered.  If
		``count`` is negative, calling this method is equivalent to calling ``ahead(-count)``.  This method has no side
		effects.
		'''

		if count < 0:
			return self.ahead(-count)

		return self.source[max(0, (self.pos - count)):self.pos]

	def advance(self, count: int = 1) -> str:
		'''
		Advances the cursor by ``count`` characters and returns a string containing the traversed characters

		The cursor might advance by less than ``count`` characters if an EOF is encountered.  If ``count`` is negative,
		calling this method is equivalent to calling ``retreat(-count)``.
		'''

		if count < 0:
			return self.retreat(-count)

		result = self.ahead(count)
		self.pos += len(result)
		return result

	def retreat(self, count: int = 1) -> str:
		'''
		Retreats the cursor by ``count`` characters and returns a string containing the traversed characters

		The cursor might retreat by less than ``count`` characters if an EOF is encountered.  If ``count`` is negative,
		calling this method is equivalent to calling ``advance(-count)``.
		'''

		if count < 0:
			return self.advance(-count)

		result = self.behind(count)
		self.pos -= len(result)
		return result

	@overload
	def match(self, match: str) -> Optional[str]:
		pass

	@overload
	def match(self, match: Callable[[str], Optional[bool]]) -> Optional[str]:
		pass

	@overload
	def match(self, match: Callable[[int, str], Optional[bool]]) -> Optional[str]:
		pass

	def match(self, match: Union[str, Callable]) -> Optional[str]:
		'''
		Advances the cursor, trying to find and return a matching string

		If ``match`` is a string, advances the cursor, this method tries to match the selected string.  If no match has
		been found, the cursor position doesn't change and ``None`` is returned.

		If ``match`` is a function accepting a string and returning an optional boolean value, this method traverses
		the source, starting at the cursor; the ``match`` function will be called for any traversed character:

		* If ``match(character) == True``, the traversal continues;

		* If ``match(character) == False``, the traversal ends and the string containing the traversed characters is
		  returned;

		* If ``match(character) == None``, the traversal is canceled and the cursor position is reset to where it was
		  before.  ``None`` is returned.

		If ``match`` is a function accepting a string and an integer and returning an optional boolean value, this
		method traverses the source, starting at the cursor; the ``match`` function will be called for any traversed
		character:

		* if match(offset, character) == True, the traversal continues;

		* if match(offset, character) == False, the traversal ends and the string containing the traversed characters
		  is returned;

		* if match(offset, character) == None, the traversal is canceled and the cursor position is reset to where it
		  was before.  ``None`` is returned.
		'''

		initial_pos = self.pos
		result = ''

		while True:
			pos = self.pos
			character = self.advance()

			if isinstance(match, str):
				offset = self.pos - initial_pos - 1
				if offset < len(match):
					matches: Optional[bool] = match[offset] == character
					if not matches:
						matches = None
				else:
					matches = False
			elif len(getfullargspec(match).args) == 1:
				matches = match(character)
			else:
				matches = match(self.pos - initial_pos - 1, character)

			if not matches:
				if matches is None:
					self.pos = initial_pos
					return None
				else:
					self.pos = pos
					break

			result += character

		return result