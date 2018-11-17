__author__ = 'Matteo Morena'
__all__ = ('Scope',)

from dataclasses import dataclass
from typing import Any, Dict, Iterator, Optional, overload

from craftlangc.enums import VarType


class Scope:
	__slots__ = frozenset({'parent', '_items'})

	parent: Optional['Scope']
	_items: Dict[str, 'Item']

	def __init__(self, parent: Optional['Scope'] = None) -> None:
		self.parent = parent
		self._items = {}

	@property
	def root(self) -> 'Scope':
		if self.parent is not None:
			return self.parent.root
		else:
			return self

	def contains(self, id: str, recursive: bool = True) -> bool:
		return self.get(id, recursive) is not None

	def get(self, id: str, recursive: bool = True) -> Optional['Item']:
		variable = self._items.get(id)
		if recursive and variable is None and self.parent is not None:
			variable = self.parent.get(id, True)
		return variable

	@overload
	def set(self, id: str, item: 'Item', recursive: bool = True) -> 'Item':
		pass

	@overload
	def set(self, id: str, type: VarType, iid_gen: Iterator[str], recursive: bool = True) -> 'Item':
		pass

	def set(self, *args: Any, **kwargs: Any) -> 'Item':
		if isinstance(args[1], self.Item):
			id = args[0]
			item = args[1]
			recursive = args[2] if len(args) > 2 else True

			if recursive:
				scope: Optional[Scope] = self
				while scope is not None and id not in scope._items:
					scope = scope.parent
				if scope is None:
					scope = self
				scope._items[id] = item
			else:
				self._items[id] = item

			return item

		else:
			id = args[0]
			type = args[1]
			iid_gen = args[2]
			recursive = args[3] if len(args) > 3 else True

			if recursive:
				scope = self
				while scope is not None and id not in scope._items:
					scope = scope.parent
				if scope is None:
					item = self.Item(next(iid_gen), type)
					self._items[id] = item
					return item
				else:
					item = scope._items[id]
					item.type = type
					return item
			else:
				if id in self._items:
					item = self._items[id]
					item.type = type
					return item
				else:
					item = self.Item(next(iid_gen), type)
					self._items[id] = item
					return item

	@dataclass
	class Item:
		iid: str
		type: VarType