__author__ = 'Matteo Morena'
__all__ = ('main',)

from argparse import ArgumentParser

from craftlangc.compile import compile_file
from craftlangc.parse import parse_file
from craftlangc.walker import Walker


def main() -> None:
	parser = ArgumentParser()
	parser.add_argument('source', help = 'the CraftLang source file to compile')
	parser.add_argument('destination', help = 'the output path')
	args = parser.parse_args()

	with open(args.source, 'rt', encoding = 'UTF-8', newline = '') as f:
		compile_file(parse_file(Walker(f.read())), args.destination)