plugins {
    id("build-logic.android.feature-library")
}

android {
    namespace = "com.mmc.merimusicplayer.common"
    testFixtures.enable = true
    buildFeatures.buildConfig = true
    defaultConfig.buildConfigField("String", "TAG", "\"[MemiPlayer]\"")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.tiny.pinyin)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
