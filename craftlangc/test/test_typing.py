__author__ = 'Matteo Morena'
__all__ = ('TestTyping',)

from unittest import TestCase

from mypy.api import run


class TestTyping(TestCase):
	def test_typing(self) -> None:
		result = run(['-p', 'craftlangc', '--strict'])
		self.assertEqual(0, result[2], result[0])
