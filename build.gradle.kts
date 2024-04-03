/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayOutputStream
import java.net.URI

buildscript {
    dependencies {
        classpath("com.github.gmazzo.buildconfig:plugin:4.1.2")
        classpath("com.github.shevek.jarjar:jarjar-gradle:master")
    }
}

plugins {
    idea
    `java-library`
    `maven-publish`
    id("com.github.gmazzo.buildconfig") version ("4.1.2")
    id("org.anarres.jarjar") version ("master")
}

group = "xyz.kafka"
version = "1.1.0"
description = "kafka connect redis."

extra.apply {
    set("lombok.version", "1.18.30")

    set("kafka.version", "3.6.0")
    set("commons.codec.version", "1.15")
    set("log4j.api.version", "2.10.0")
    set("slf4j.api.version", "2.0.5")

    set("junit.jupiter.version", "5.9.2")

    set("confluent.version", "7.6.0")
    set("scala.version", "2.13.11")
    set("jackson.version", "2.15.2")
    set("connect.utils.version", "0.7.177")
    set("connect.storage.version", "11.2.3")
    set("http.components.version", "4.5.14")

    set("xyz.kafka.connector.version", "1.0.0")
}

val mongoDependencies: Configuration by configurations.creating
val mongoAndAvroDependencies: Configuration by configurations.creating
val projectName = "kafka-connect-redis"

dependencies {
    implementation("com.github.jcustenborder.kafka.connect:connect-utils:${project.extra["connect.utils.version"]}") {
        exclude(group = "org.apache.kafka", module = "connect-api")
        exclude(group = "org.freemarker", module = "freemarker")
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.immutables", module = "value")
    }
    compileOnly("xyz.kafka.connector:kafka-connector-common:${project.extra["xyz.kafka.connector.version"]}")
    compileOnly("org.apache.kafka:kafka-streams:${project.extra["kafka.version"]}")
    compileOnly("org.apache.kafka:kafka-clients:${project.extra["kafka.version"]}")
    compileOnly("org.apache.kafka:connect-api:${project.extra["kafka.version"]}")
    compileOnly("commons-codec:commons-codec:${project.extra["commons.codec.version"]}")
    compileOnly("org.apache.logging.log4j:log4j-api:${project.extra["log4j.api.version"]}")
    compileOnly("org.slf4j:slf4j-api:${project.extra["slf4j.api.version"]}")
    compileOnly("org.projectlombok:lombok:${project.extra["lombok.version"]}")
    annotationProcessor("org.projectlombok:lombok:${project.extra["lombok.version"]}")

    testImplementation("com.github.jcustenborder.kafka.connect:connect-utils:${project.extra["connect.utils.version"]}")
    testImplementation("xyz.kafka.connector:kafka-connector-common:${project.extra["xyz.kafka.connector.version"]}")
    testImplementation("org.apache.kafka:kafka-streams:${project.extra["kafka.version"]}")
    testImplementation("org.apache.kafka:kafka-clients:${project.extra["kafka.version"]}")
    testImplementation("org.apache.kafka:connect-api:${project.extra["kafka.version"]}")
    testImplementation("commons-codec:commons-codec:${project.extra["commons.codec.version"]}")

    testImplementation(platform("org.junit:junit-bom:${project.extra["junit.jupiter.version"]}"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.junit.platform:junit-platform-runner:1.9.2")
    // Integration Tests
    testImplementation("org.scala-lang:scala-library:${project.extra["scala.version"]}")
    testImplementation("org.apache.logging.log4j:log4j-api:${project.extra["log4j.api.version"]}")
    testImplementation("org.slf4j:slf4j-api:${project.extra["slf4j.api.version"]}")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17)
}

val defaultJdkVersion = 17
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(defaultJdkVersion))
    }
}

/*
 * Generated files
 */
val gitVersion: String by lazy {
    val describeStdOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "describe", "--tags", "--always", "--dirty")
        standardOutput = describeStdOut
    }
    describeStdOut.toString().substring(1).trim()
}

val gitDiffNameOnly: String by lazy {
    val describeStdOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "diff", "--name-only")
        standardOutput = describeStdOut
    }
    describeStdOut.toString().replaceIndent(" - ")
}

buildConfig {
    className("Versions")
    packageName("xyz.kafka.connect.redis")
    useJavaOutput()
    buildConfigField("String", "NAME", "\"${projectName}\"")
    buildConfigField("String", "VERSION", provider { "\"${gitVersion}\"" })
}

tasks.withType<Test> {
    val addOpensText = project.property("add.opens") as String
    val addOpens = addOpensText.split(" ")
    jvmArgs(addOpens)

    tasks.getByName("check").dependsOn(this)
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }

    val javaVersion: Int = (project.findProperty("javaVersion") as String? ?: defaultJdkVersion.toString()).toInt()
    logger.info("Running tests using JDK$javaVersion")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    })

    val jdkHome = project.findProperty("jdkHome") as String?
    jdkHome.let {
        val javaExecutablesPath = File(jdkHome, "bin/java")
        if (javaExecutablesPath.exists()) {
            executable = javaExecutablesPath.absolutePath
        }
    }

    addTestListener(object : TestListener {
        override fun beforeTest(testDescriptor: TestDescriptor?) {}
        override fun beforeSuite(suite: TestDescriptor?) {}
        override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) {}
        override fun afterSuite(d: TestDescriptor?, r: TestResult?) {
            if (d != null && r != null && d.parent == null) {
                val resultsSummary = """Tests summary:
                    | ${r.testCount} tests,
                    | ${r.successfulTestCount} succeeded,
                    | ${r.failedTestCount} failed,
                    | ${r.skippedTestCount} skipped""".trimMargin().replace("\n", "")

                val border = "=".repeat(resultsSummary.length)
                logger.lifecycle("\n$border")
                logger.lifecycle("Test result: ${r.resultType}")
                logger.lifecycle(resultsSummary)
                logger.lifecycle("${border}\n")
            }
        }
    })

}


tasks.named("compileJava") {
}

/*
 * Publishing
 */
tasks.register<Jar>("sourcesJar") {
    description = "Create the sources jar"
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

tasks.register<Jar>("javadocJar") {
    description = "Create the Javadoc jar"
    from(tasks.javadoc)
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = projectName
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://www.pistonint.com")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("Various")
                        organization.set("Piston")
                    }
                    developer {
                        id.set("Hans-Peter Grahsl")
                    }
                }
                scm {
                    connection.set("scm:https://lab.pistonint.com/big-data/${projectName}.git")
                    developerConnection.set("scm:git@lab.pistonint.com:8022/big-data/${projectName}.git")
                    url.set("https://lab.pistonint.com/big-data/${projectName}.git")
                }
            }
        }
    }

    repositories {
        maven {
            url = URI.create(System.getenv("MAVEN_RELEASE_ENDPOINT"))
            isAllowInsecureProtocol = true
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_PWD")
            }
        }
    }
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

tasks.register("publishSnapshots") {
    group = "publishing"
    description = "Publishes snapshots to Sonatype"
    if (version.toString().endsWith("-SNAPSHOT")) {
        dependsOn(tasks.withType<PublishToMavenRepository>())
    }
}

tasks.register("publishArchives") {
    group = "publishing"
    description = "Publishes a release and uploads to Sonatype / Maven Central"

    doFirst {
        if (gitVersion != version) {
            val cause = """
                | Version mismatch:
                | =================
                |
                | $version != $gitVersion
                |
                | Modified Files:
                |$gitDiffNameOnly
                |
                | The project version does not match the git tag.
                |""".trimMargin()
            throw GradleException(cause)
        } else {
            println("Publishing: ${project.name} : $gitVersion")
        }
    }

    if (gitVersion == version) {
        dependsOn(tasks.withType<PublishToMavenRepository>())
    }
}