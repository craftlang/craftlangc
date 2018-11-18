__author__ = 'Matteo Morena'
__all__ = ('TestWalker',)

from unittest import TestCase

from craftlangc.character import is_identifier_continue
from craftlangc.walker import Walker


class TestWalker(TestCase):
	walker: Walker

	def setUp(self) -> None:
		self.walker = Walker("Hello\nhow\rare\r\nyou?\r\n\r\r\nI'm fine :D")

	def test_line_column(self) -> None:
		self.assertEqual(0, self.walker.line)
		self.assertEqual(0, self.walker.column)

		self.walker.pos = 3
		self.assertEqual(0, self.walker.line)
		self.assertEqual(3, self.walker.column)

		self.walker.pos = 9
		self.assertEqual(1, self.walker.line)
		self.assertEqual(3, self.walker.column)

		self.walker.pos = 100
		self.assertEqual(35, self.walker.pos)
		self.assertEqual(6, self.walker.line)
		self.assertEqual(11, self.walker.column)

	def test_set_pos(self) -> None:
		self.walker.pos = 16
		self.assertEqual('ou', self.walker.advance(2))
		self.assertEqual(18, self.walker.pos)
		self.assertEqual(3, self.walker.line)
		self.assertEqual(3, self.walker.column)

	def test_ahead(self) -> None:
		self.walker.pos = 2
		self.assertEqual('', self.walker.ahead(0))
		self.assertEqual('llo\nho', self.walker.ahead(6))
		self.assertEqual("llo\nhow\rare\r\nyou?\r\n\r\r\nI'm fine :D", self.walker.ahead(100))
		self.assertEqual(2, self.walker.pos)

	def test_behind(self) -> None:
		self.walker.advance(4)
		pos = self.walker.pos
		self.assertEqual(self.walker.ahead(3), self.walker.behind(-3))
		self.assertEqual('ell', self.walker.behind(3))
		self.assertEqual(pos, self.walker.pos)

	def test_advance(self) -> None:
		self.assertEqual('Hello', self.walker.advance(5))
		self.assertEqual(5, self.walker.pos)

		self.assertEqual('', self.walker.advance(0))
		self.assertEqual(5, self.walker.pos)

		self.assertEqual('\nh', self.walker.advance(2))
		self.assertEqual(7, self.walker.pos)

		self.assertEqual('ow\rare', self.walker.advance(6))
		self.assertEqual(13, self.walker.pos)

		self.assertEqual("\r\nyou?\r\n\r\r\nI'm fine :D", self.walker.advance(100))
		self.assertGreater(113, self.walker.pos)

	def test_retreat(self) -> None:
		self.walker.pos = 5
		self.assertEqual('', self.walker.retreat(0))
		self.assertEqual('llo', self.walker.retreat(3))
		self.assertEqual(2, self.walker.pos)
		self.assertEqual(0, self.walker.line)
		self.assertEqual(2, self.walker.column)

		self.walker.pos = 18
		self.assertEqual('\nyou', self.walker.retreat(4))
		self.assertEqual(14, self.walker.pos)
		self.assertEqual(2, self.walker.line)
		self.assertEqual(3, self.walker.column)

		self.assertEqual('\r', self.walker.retreat(1))
		self.assertEqual(13, self.walker.pos)
		self.assertEqual(2, self.walker.line)
		self.assertEqual(3, self.walker.column)

		self.assertEqual('Hello\nhow\rare', self.walker.retreat(100))
		self.assertEqual(0, self.walker.pos)
		self.assertEqual(0, self.walker.line)
		self.assertEqual(0, self.walker.column)

	def test_match(self) -> None:
		self.assertIsNone(self.walker.match('Hell0'))
		self.assertEqual('Hello', self.walker.match('Hello'))
		self.assertEqual(5, self.walker.pos)

		self.walker.advance()
		self.assertEqual('how', self.walker.match(is_identifier_continue))
		self.assertEqual(9, self.walker.pos)
