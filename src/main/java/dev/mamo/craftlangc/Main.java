package dev.mamo.craftlangc;

import dev.mamo.craftlangc.Compiler.*;
import dev.mamo.craftlangc.Parser.*;
import dev.mamo.craftlangc.ast.*;
import dev.mamo.craftlangc.core.*;
import dev.mamo.craftlangc.core.parser.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;

public class Main {
	private static final String TAB = "\t";
	private static final String NL_REGEX = "(?:\r\n|\r|\n)";

	private Main() {}

	public static void main(List<String> args) {
		List<Path> sources = new ArrayList<>();
		Path destination = null;
		boolean force = false;
		boolean zip = false;

		for (int i = 0, argCount = args.size(); i < argCount; i++) {
			String arg = args.get(i);

			if (arg.startsWith("--")) {
				switch (arg) {
					case "--help":
						printHelp(System.out);
						return;
					case "--output":
						if (i + 1 < argCount) {
							destination = Paths.get(args.get(i + 1));
						} else {
							printHelp(System.err);
							System.exit(1);
						}
						break;
					case "--force":
						force = true;
						break;
					case "--zip":
						zip = true;
						break;
					default:
						System.err.println("Invalid option: " + Utils.quote(arg) + '.');
						System.err.println("Try \"craftlangc --help\" for more information.");
						System.exit(1);
						break;
				}
			} else if (arg.startsWith("-")) {
				boolean skipNext = false;

				for (int j = 1, argLength = arg.length(); j < argLength; j++) {
					char character = arg.charAt(j);

					switch (character) {
						case 'h':
							printHelp(System.out);
							return;
						case 'o':
							if (i + 1 < argCount) {
								destination = Paths.get(args.get(i + 1));
								skipNext = true;
							} else {
								printHelp(System.err);
								System.exit(1);
							}
							break;
						case 'f':
							force = true;
							break;
						case 'z':
							zip = true;
							break;
						default:
							System.err.println("Invalid option: " + Utils.quote(Character.toString(character)) + '.');
							System.err.println("Try \"craftlangc --help\" for more information.");
							System.exit(1);
							break;
					}
				}

				if (skipNext) {
					i++;
				}
			} else {
				sources.add(Paths.get(arg));
			}
		}

		if (destination == null) {
			if (!sources.isEmpty()) {
				int lastIndex = sources.size() - 1;
				destination = sources.get(lastIndex);
				sources.remove(lastIndex);
			} else {
				printHelp(System.err);
				System.exit(1);
			}
		}

		if (Files.exists(destination)) {
			if (force) {
				boolean isDirectory = Files.isDirectory(destination);
				try {
					Utils.delete(destination);
				} catch (IOException ex) {
					System.err.println("I/O error while deleting destination " + (isDirectory ? "directory" : "file") + ": " + ex.getMessage());
					System.exit(1);
				}
			} else {
				System.err.println("Error: destination already exists");
				System.exit(1);
			}
		}

		List<Unit> units = new ArrayList<>();

		for (Path source : sources) {
			try {
				Files.walk(source).forEach(p -> {
					ParseContext context;

					try {
						context = new ParseContext(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
					} catch (IOException ex) {
						System.err.println("I/O error while reading source file " + source + ": " + ex.getMessage());
						System.exit(1);
						return;
					}

					try {
						units.add(Parser.parse(context));
					} catch (ParseException ex) {
						System.err.println("Error while parsing source file " + source + ':');
						System.err.println("[" + Utils.getRC(context.getSource(), ex.getPosition(), NL_REGEX) + "] " + ex.getMessage());
						System.exit(1);
					}
				});
			} catch (IOException ex) {
				System.err.println("I/O error while reading source file " + source + ": " + ex.getMessage());
				System.exit(1);
			}
		}

		try {
			if (zip) {
				Files.createDirectories(destination.toAbsolutePath().getParent());
			} else {
				Files.createDirectories(destination);
			}
		} catch (IOException ex) {
			System.err.println("I/O error while creating destination directory: " + ex.getMessage());
			System.exit(1);
		}

		try {
			if (zip) {
				try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + destination.toUri()), Utils.mapOf("create", "true"))) {
					Compiler.compile(fs.getPath(""), units);
				}
			} else {
				Compiler.compile(destination, units);
			}
		} catch (CompileException ex) {
			System.err.println("Error while compiling:");
			System.err.println("[" + ex.getPosition() + "] " + ex.getMessage());
			try {
				Utils.delete(destination);
			} catch (IOException ignored) {}
			System.exit(1);
		} catch (IOException ex) {
			System.err.println("I/O error while writing to destination: " + ex.getMessage());
			try {
				Utils.delete(destination);
			} catch (IOException ignored) {}
			System.exit(1);
		}
	}

	public static void main(String... args) {
		main(Arrays.asList(args));
	}

	private static void printHelp(PrintStream out) {
		out.println("Usage: craftlangc [options] <sources> [-o] <destination>");
		out.println("Compiler for the Craftlang programming language");
		out.println();
		out.println("Options:");
		out.println("-f, --force               " + TAB + "If set, overwrites the output if necessary");
		out.println("-h, --help                " + TAB + "Displays this help message and exits");
		out.println("-o <path>, --output <path>" + TAB + "Selects the destination path");
		out.println("-z, --zip                 " + TAB + "If set, outputs to zip");
	}
}