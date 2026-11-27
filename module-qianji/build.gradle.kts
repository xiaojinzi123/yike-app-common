plugins {
    id("moduleBusinessPlugin")
}

android {
    namespace = "com.xiaojinzi.tally.module.qianji"
    buildFeatures { aidl = true }
}

dependencies {
    implementation(project(":module-datasource"))
    implementation(project(":module-base"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    testImplementation(libs.junit)
}
