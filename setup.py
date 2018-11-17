__author__ = 'Matteo Morena'
__all__ = ()

from setuptools import find_packages, setup

setup(
	name = 'craftlangc',
	version = '0.1.0',
	description = 'The compiler for the CraftLang programming language',
	author = 'Matteo Morena',
	keywords = 'craftlang compiler minecraft dsl mcfunction datapack',
	classifiers = [
		'Intended Audience :: End Users/Desktop',
		'Natural Language :: English',
		'Operating System :: OS Independent',
		'Topic :: Software Development :: Compilers'
	],
	packages = find_packages(),
	tests_require = [
		'mypy'
	]
)