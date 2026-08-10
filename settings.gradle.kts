rootProject.name = "tickethub"

include("booking-service")
include("api-gateway")
include("ticket-service")
include("event-service")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}