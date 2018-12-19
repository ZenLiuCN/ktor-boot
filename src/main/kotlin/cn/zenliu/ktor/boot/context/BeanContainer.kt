package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.Controller
import cn.zenliu.ktor.boot.annotations.context.Name
import cn.zenliu.ktor.boot.annotations.context.Singleton
import cn.zenliu.ktor.boot.annotations.routing.RawRoute
import cn.zenliu.ktor.boot.reflect.*
import io.ktor.application.Application
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

internal data class BeanContainer(
    val pkg: String = "",
    val name: String = "",
    val path: String = "",
    val clazz: KClass<*>
) {
    val annotations: List<Annotation> = clazz.annotationsSafe.toList()
    val isSingleton: Boolean by lazy { clazz.findAnnotationSafe<Singleton>()?.let { true } ?: false }
    val isController: Boolean  by lazy { clazz.findAnnotationSafe<Controller>()?.let { true } ?: false }
    val routeFunctions by lazy { clazz.functionsSafe.filter { it.findAnnotation<RawRoute>()!=null } }
    val alias: String? by lazy { (annotations.find { it is Name } as? Name)?.name }
    val isConfigurationClass: Boolean by lazy {
        clazz.findAnnotationSafe<cn.zenliu.ktor.boot.annotations.context.Configuration>() != null && clazz.isSubclassOf(Configuration::class)
    }
    val hasConfigurationFunction: Boolean by lazy {
        clazz.declaredFunctionsSafe.any {
            (it.annotations.find { it is Configuration } != null) && it.parameters.find {
                it.type.isClass(
                    Application::class
                )
            } != null
        }
    }

    fun configurationFunctions(): Set<KFunction<*>> = clazz.declaredFunctions
        .filter {
            (it.annotations.find { it is Configuration } != null) && it.parameters.find {
                it.kind == KParameter.Kind.VALUE && it.type.isClass(
                    Application::class
                )
            } != null
        }.toSet()

}
