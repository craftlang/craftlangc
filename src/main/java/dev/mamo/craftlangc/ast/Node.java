package dev.mamo.craftlangc.ast;

import dev.mamo.craftlangc.core.parser.*;

public interface Node {
	ParseNode getSource();

	void setSource(ParseNode node);
}