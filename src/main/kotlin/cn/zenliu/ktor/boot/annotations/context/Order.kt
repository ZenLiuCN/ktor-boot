package cn.zenliu.ktor.boot.annotations.context


/**
 *  marker of class loading or configuration used order
 * @property value Int
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class Order(val value: Int = Int.MAX_VALUE)
