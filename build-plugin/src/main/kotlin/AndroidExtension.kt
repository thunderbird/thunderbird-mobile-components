import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

internal fun CommonExtension<*, *, *, *, *, *>.configureSharedConfig(project: Project) {
    compileSdk = ThunderbirdProjectConfig.Android.sdkCompile

    defaultConfig {
        compileSdk = ThunderbirdProjectConfig.Android.sdkCompile
        minSdk = ThunderbirdProjectConfig.Android.sdkMin
    }

    compileOptions {
        sourceCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
        targetCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = true
        lintConfig = project.file("${project.rootProject.projectDir}/config/lint/lint.xml")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
            )
        }
    }
}
