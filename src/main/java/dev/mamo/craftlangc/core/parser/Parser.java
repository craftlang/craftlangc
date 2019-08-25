package dev.mamo.craftlangc.core.parser;

@FunctionalInterface
public interface Parser {
	ParseNode parse(ParseContext context);
}