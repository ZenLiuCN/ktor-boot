package cn.zenliu.ktor.boot.annotations.request

/**
 * mark one route function parameter should be as request url path variable
 * @property name String
 * @constructor
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class PathVariable(val name: String = "")
