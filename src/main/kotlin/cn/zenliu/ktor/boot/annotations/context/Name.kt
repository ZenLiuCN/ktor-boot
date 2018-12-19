package cn.zenliu.ktor.boot.annotations.context

/**
 * define bean class alias
 * @property name String
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class Name(val name: String)

