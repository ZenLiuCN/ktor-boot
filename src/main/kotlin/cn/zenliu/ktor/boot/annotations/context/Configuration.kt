package cn.zenliu.ktor.boot.annotations.context


@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Configuration(val order: Int = 0)

