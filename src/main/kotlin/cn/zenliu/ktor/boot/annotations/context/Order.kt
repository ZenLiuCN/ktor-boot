package cn.zenliu.ktor.boot.annotations.context


/**
 * define order
 * @property value Int
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class Order(val value: Int = Int.MAX_VALUE)
