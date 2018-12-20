package cn.zenliu.ktor.boot.annotations.context

/**
 * marker of a data class will be defined in HOCON <application.conf>
 * @property path String
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class Properties(val path: String)
