package dev.mamo.craftlangc.core;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class Util {
	private Util() {}

	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> map(List<Object> entries) {
		Map<K, V> result = new HashMap<>();
		for (int i = 0; i < entries.size(); i += 2) {
			result.put((K) entries.get(i), (V) entries.get(i + 1));
		}
		return result;
	}

	public static <K, V> Map<K, V> map(Object... entries) {
		return map(Arrays.asList(entries));
	}

	public static String quote(String string) {
		StringBuilder result = new StringBuilder("\"");

		for (int i = 0, length = string.length(); i < length; i++) {
			char c = string.charAt(i);
			switch (c) {
				case '\t':
					result.append("\\t");
					break;
				case '\b':
					result.append("\\b");
					break;
				case '\n':
					result.append("\\n");
					break;
				case '\r':
					result.append("\\r");
					break;
				case '\f':
					result.append("\\f");
					break;
				case '"':
					result.append("\\\"");
					break;
				case '\\':
					result.append("\\\\");
					break;
				default:
					if (Character.isISOControl(c)) {
						result.append(String.format("\\u%04X", (short) c));
					} else {
						result.append(c);
					}
					break;
			}
		}

		return result.append("\"").toString();
	}

	public static String toBase62(int integer) {
		StringBuilder result = new StringBuilder();
		for (; integer > 0; integer /= 62) {
			int remainder = integer % 62;
			if (remainder <= 9) {
				result.insert(0, remainder);
			} else if (remainder <= 35) {
				result.insert(0, (char) (remainder + 55));
			} else {
				result.insert(0, (char) (remainder + 61));
			}
		}
		return result.length() > 0 ? result.toString() : "0";
	}

	public static RC getRC(String string, int position, String nlRegex) {
		String[] lines = string.substring(0, position + 1).split(nlRegex);
		int length = lines.length;
		return new RC(length, length > 0 ? lines[length - 1].length() : 0);
	}

	public static RC getRC(String string, int position) {
		return getRC(string, position, "\\R");
	}

	public static void delete(Path path) throws IOException {
		if (Files.exists(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return super.visitFile(file, attrs);
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return super.postVisitDirectory(dir, exc);
				}
			});
		}
	}
}