package cn.zenliu.ktor.boot.annotations.context

@Target(AnnotationTarget.CLASS)
annotation class Controller(val name: String = "", val order: Int = 0)

