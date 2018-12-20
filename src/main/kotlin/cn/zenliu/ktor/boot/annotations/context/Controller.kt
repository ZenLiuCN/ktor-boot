package cn.zenliu.ktor.boot.annotations.context

/**
 * marker for class contains route functions
 * [cn.zenliu.ktor.boot.annotations.routingRawRoute], [cn.zenliu.ktor.boot.annotations.routing.RequestMapping]
 * @property name String
 * @property order Int
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class Controller(val name: String = "", val order: Int = 0)

