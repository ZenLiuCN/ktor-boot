package cn.zenliu.ktor.boot.annotations.request

/**
 *  mark one route function parameter should be as request Query Parameter
 * @property name String
 * @constructor
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class QueryParam(val name: String = "")
