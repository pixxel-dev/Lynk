plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // Add domain-specific Java dependencies here
}

tasks.register<JavaExec>("runAudit") {
    group = "verification"
    description = "Runs the architectural rules audit"
    mainClass.set("com.example.lynk.core.domain.audit.rules.AuditorRunner")
    classpath = sourceSets["main"].runtimeClasspath
}
