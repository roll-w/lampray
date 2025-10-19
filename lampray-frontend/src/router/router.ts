import "vue-router"

export {}

declare module "vue-router" {
    interface RouteMeta {
        requireAdmin?: boolean
        requiresLogin?: boolean
    }
}