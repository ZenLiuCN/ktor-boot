package cn.zenliu.ktor.boot.annotations.request

/**
 * mark one route function parameter should be as request body
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Body
