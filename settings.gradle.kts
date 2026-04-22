rootProject.name = "simple-banking"
// подключаем все модули из папки services
file("services").listFiles()?.forEach { dir ->
    if (dir.isDirectory && File(dir, "build.gradle.kts").exists()) {
        include("services:${dir.name}")
    }
}