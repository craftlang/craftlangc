__author__ = 'Matteo Morena'
__all__ = ('is_newline', 'is_whitespace', 'is_identifier_start', 'is_identifier_continue', 'is_digit')

from unicodedata import category


def is_newline(c: str) -> bool:
	return c in {'\n', '\r', '\v', '\f', '\r', '\x85', '\u2028', '\u2029'}


def is_whitespace(c: str) -> bool:
	return c in {
		'\t', '\x11', ' ', '\xa0', '\u1680', '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006',
		'\u2007', '\u2008', '\u2009', '\u200a', '\u202f', '\u205f', '\u3000'
	}


def is_identifier_start(c: str) -> bool:
	return len(c) == 1 and category(c) in {'Lu', 'Ll', 'Lt', 'Lm', 'Lo', 'Pc'}


def is_identifier_continue(c: str) -> bool:
	return len(c) == 1 and (is_identifier_start(c) or category(c) in {'Mn', 'Mc', 'Nd', 'Nl'})


def is_digit(c: str) -> bool:
	return c in {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}