plugins {
	java
	id("io.papermc.paperweight.userdev") version "1.3.3"
}

repositories {
	maven { url = uri("https://papermc.io/repo/repository/maven-public/")}
	mavenCentral()
}

dependencies {
	compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")
	paperDevBundle("1.18.1-R0.1-SNAPSHOT")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}