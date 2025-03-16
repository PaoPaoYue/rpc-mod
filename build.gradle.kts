plugins {
    id("java")
    id("org.springframework.boot") version ("2.7.18")
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    id("io.github.paopaoyue.ypp-rpc-generator") version "0.1.0-jdk8"
}

group = "com.github.paopaoyue"
version = "0.1.4"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

configurations {
    all {
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
}

sourceSets {
    val env = project.findProperty("env")?.toString() ?: "dev"

    main {
        resources {
            setSrcDirs(listOf("src/main/resources/$env"))
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.squareup.okhttp3:okhttp:3.14.9")


    implementation(files("C:\\Users\\LENOVO\\Desktop\\projects\\mtsLib\\BaseMod.jar"))
    implementation(files("C:\\Users\\LENOVO\\Desktop\\projects\\mtsLib\\desktop-1.0.jar"))
    implementation(files("C:\\Users\\LENOVO\\Desktop\\projects\\mtsLib\\ModTheSpire.jar"))
    implementation(files("C:\\Users\\LENOVO\\Desktop\\projects\\mtsLib\\StSLib.jar"))

}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveClassifier.set("stsMod")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations["compileClasspath"].forEach { _: File ->
        from({
            configurations.compileClasspath.get().filter {
                !(it.name.endsWith("BaseMod.jar") ||
                        it.name.endsWith("desktop-1.0.jar") ||
                        it.name.endsWith("ModTheSpire.jar") ||
                        it.name.endsWith("StSLib.jar"))
            }.map { zipTree(it) }
        }) {
            exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/LICENSE.txt", "META-INF/NOTICE.txt")
        }
    }
}

tasks.register<Delete>("cleanDevFolder") {
    delete(fileTree("E:\\Steam Games\\steamapps\\common\\SlayTheSpire\\mods") { include("*.jar") })
}

tasks.register<Delete>("cleanPublishFolder") {
    delete(fileTree("E:\\Steam Games\\steamapps\\common\\SlayTheSpire\\rpc-mod\\content") { include("*.jar") })
}

tasks.register<Copy>("deployToDevFolder") {
    dependsOn(tasks.clean, tasks.jar)
    from(layout.buildDirectory.dir("libs"))
    include("*.jar")
    into(layout.buildDirectory.dir("E:\\Steam Games\\steamapps\\common\\SlayTheSpire\\mods"))
}

tasks.register<Copy>("deployToPublishFolder") {
    dependsOn(tasks.clean, tasks.jar)
    from(layout.buildDirectory.dir("libs"))
    include("*.jar")
    into(layout.buildDirectory.dir("E:\\Steam Games\\steamapps\\common\\SlayTheSpire\\rpc-mod\\content"))
}

tasks.register<Exec>("runGame") {
    dependsOn(":deployToTestFolder")
    workingDir("E:\\Steam Games\\steamapps\\common\\SlayTheSpire")
    commandLine("cmd", "/c", "java -jar mts-launcher.jar")
}

tasks.register<Exec>("publishMod") {
    dependsOn(":cleanPublishFolder", ":deployToPublishFolder")
    workingDir("E:\\Steam Games\\steamapps\\common\\SlayTheSpire")
    commandLine("cmd", "/c", "java -jar mod-uploader.jar upload -w rpc-mod")
}