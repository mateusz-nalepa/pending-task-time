rootProject.name = "pending-task-time-poc"


include(
    ":request-sender",
    ":webflux-defaults-app",
    ":webflux-undertow-server-client-netty",
    ":mock-external-service"
)