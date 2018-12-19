package cn.zenliu.ktor.boot.annotations.request

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(val name: String = "")
