plugins {
	java
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
	compileJava {
		with(options) {
			isDebug = !project.hasProperty("release")
			isDeprecation = true
			encoding = "UTF-8"
		}
	}

	jar {
		manifest {
			attributes("Main-Class" to "dev.mamo.craftlangc.Main")
		}
		entryCompression = ZipEntryCompression.STORED
		isPreserveFileTimestamps = false
		isReproducibleFileOrder = true
	}

	register<JavaExec>("run") {
		group = "application"
		description = "Runs this project as a JVM application"
		classpath = sourceSets["main"].runtimeClasspath
		main = "dev.mamo.craftlangc.Main"
	}
}