plugins {
    id("moduleBusinessPlugin")
}

android {
    namespace = "com.xiaojinzi.tally.module.qianji"
}

dependencies {
    implementation(project(":module-datasource"))
    implementation(project(":module-base"))
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("org.apache.poi:poi:5.2.5")
}
