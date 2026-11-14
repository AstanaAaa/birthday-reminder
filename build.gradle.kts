// 根构建脚本：只声明插件版本，不参与编译
plugins {
    // Android Gradle Plugin：负责把 Kotlin 代码打包成 apk
    id("com.android.application") version "8.2.2" apply false
    // Kotlin 编译器
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
