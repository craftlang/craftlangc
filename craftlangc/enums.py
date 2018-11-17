__author__ = 'Matteo Morena'
__all__ = ('VarType',)

from enum import Enum, auto, unique


@unique
class VarType(Enum):
	VOID = auto()
	BOOLEAN = auto()
	SCORE = auto()
	ENTITY = auto()

	@classmethod
	def from_str(cls, s: str) -> 'VarType':
		return {
			'void': VarType.VOID,
			'boolean': VarType.BOOLEAN,
			'score': VarType.SCORE,
			'entity': VarType.ENTITY
		}[s]