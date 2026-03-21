plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "DuDoong"
include("DuDoong-Domain")
include("DuDoong-Infrastructure")
include("DuDoong-Admin")
include("DuDoong-Api")
include("DuDoong-Common")
include("DuDoong-Batch")
