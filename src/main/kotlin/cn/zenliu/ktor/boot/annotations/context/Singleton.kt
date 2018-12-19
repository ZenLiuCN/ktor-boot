package cn.zenliu.ktor.boot.annotations.context

@Target(AnnotationTarget.CLASS)
annotation class Singleton(val name: String = "")

