package cn.zenliu.ktor.boot.annotations.context

/**
 * mark property should generate from BeanManager
 * @property name String
 * @constructor
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
annotation class AutoWire(val name: String)

