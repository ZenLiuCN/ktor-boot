package cn.zenliu.ktor.boot.annotations.context

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
annotation class AutoWire(val name: String)

