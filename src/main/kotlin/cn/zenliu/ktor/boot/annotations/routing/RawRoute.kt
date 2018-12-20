package cn.zenliu.ktor.boot.annotations.routing

import cn.zenliu.ktor.boot.annotations.context.Ignore

/**
 * define a RawRoute configuration function
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
@Ignore
annotation class RawRoute
