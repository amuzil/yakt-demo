/*
 * Developed by Mahtaran & the Amuzil community in 2023
 * Any copyright is dedicated to the Public Domain.
 * <https://unlicense.org>
 */
import com.amuzil.yakt.YAKTTask
import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.internal.os.OperatingSystem

buildscript {
	repositories {
		mavenCentral()
		flatDir { dirs("libs") }
	}

	dependencies {
		classpath("com.amuzil.yakt:yakt:1.0.0") { isChanging = true }
		classpath(explainLibs.jackson.core)
	}
}

/*
Kotlin will warn us that our version catalogs can't be called by the implicit receiver.
This is, however, not the case, as we can call them just fine, so we suppress those warnings.
*/
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
	// build
	kotlin("jvm") version buildLibs.versions.kotlin.get()
	// check
	alias(checkLibs.plugins.spotless)
	// explain
	// publish
}

apply(plugin = "com.amuzil.yakt")

/* * * * * *
 * GENERAL *
 * * * * * */

group = "com.amuzil.yakt"

version = "1.0.0"

description = "A demo of the YAKT plugin."

/* * * * *
 * BUILD *
 * * * * */

repositories { mavenCentral() }

dependencies {}

kotlin { jvmToolchain(buildLibs.versions.java.get().toInt()) }

/* * * * *
 * CHECK *
 * * * * */

spotless {
	// Load from file and replace placeholders
	val licenseHeaderKt =
		file("license-header.kt").readText().replace("\$AUTHORS", "Mahtaran & the Amuzil community")
	val prettierConfig = ".prettierrc"

	ratchetFrom("origin/main")

	kotlin {
		ktfmt().kotlinlangStyle()
		indentWithTabs(4)
		licenseHeader(licenseHeaderKt).yearSeparator("–")
		toggleOffOn()
	}

	kotlinGradle {
		ktfmt().kotlinlangStyle()
		indentWithTabs(4)
		licenseHeader(licenseHeaderKt, "(pluginManagement |import )").yearSeparator("–")
		toggleOffOn()
	}

	java {
		palantirJavaFormat()
		indentWithTabs(4)
		licenseHeader(licenseHeaderKt).yearSeparator("–")
		toggleOffOn()
	}

	json {
		target("src/**/*.json", prettierConfig)

		prettier(mapOf("prettier" to checkLibs.versions.prettier.asProvider().get()))
			.configFile(prettierConfig)
	}

	format("toml") {
		target("**/*.toml")

		prettier(
				mapOf(
					"prettier" to checkLibs.versions.prettier.asProvider().get(),
					"prettier-plugin-toml" to checkLibs.versions.prettier.toml.get()
				)
			)
			.configFile(prettierConfig)
	}

	format("markdown") {
		target("**/*.md")

		prettier(mapOf("prettier" to checkLibs.versions.prettier.asProvider().get()))
			.configFile(prettierConfig)
			.config(mapOf("tabWidth" to 2, "useTabs" to false))
	}

	format("yaml") {
		target("**/*.yml", "**/*.yaml")

		prettier(mapOf("prettier" to checkLibs.versions.prettier.asProvider().get()))
			.configFile(prettierConfig)
			.config(mapOf("tabWidth" to 2, "useTabs" to false))
	}
}

val format by
	tasks.registering(Task::class) {
		group = "verification"
		description = "Runs the formatter on the project"

		dependsOn(tasks.spotlessApply)
	}

/* * * * * *
 * EXPLAIN *
 * * * * * */

tasks.withType<YAKTTask> {
	destination.set(file("CHANGELOG.md"))

	scraping { tagPrefix.set("v") }
}

val changelog by
	tasks.registering(Exec::class) {
		group = "changelog"
		description = "Generates a changelog for the current version. Requires PNPM"

		workingDir = project.rootDir

		ObjectMapper()
			.writeValue(
				file(".gitmoji-changelogrc"),
				mapOf(
					"project" to
						mapOf(
							"name" to "ForgeKonfig",
							"description" to project.description,
							"version" to project.version
						)
				)
			)

		val command =
			listOf(
				// spotless:off
                "pnpx", "gitmoji-changelog",
                "--format", "markdown",
                "--preset", "generic",
                "--output", "changelog.md",
                "--group-similar-commits", "true",
                "--author", "true"
                // spotless:on
			)

		with(OperatingSystem.current()) {
			when {
				isWindows -> commandLine(listOf("cmd", "/c") + command)
				isLinux -> commandLine(command)
				else -> throw IllegalStateException("Unsupported operating system: $this")
			}
		}

		finalizedBy("spotlessMarkdownApply")
	}

/* * * * * *
 * PUBLISH *
 * * * * * */
