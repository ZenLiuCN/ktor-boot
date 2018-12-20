package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.Controller
import cn.zenliu.ktor.boot.annotations.context.Alias
import cn.zenliu.ktor.boot.annotations.context.Ignore
import cn.zenliu.ktor.boot.annotations.routing.RawRoute
import cn.zenliu.ktor.boot.reflect.*
import io.ktor.application.Application
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

/**
 * BeanContainer contain a KClass infomation
 * @property pkg String
 * @property name String
 * @property path String
 * @property clazz KClass<*>
 * @property annotations List<Annotation>
// * @property isSingleton Boolean
 * @property isController Boolean
 * @property routeFunctions List<KFunction<*>>
 * @property alias String?
 * @property isConfigurationClass Boolean
 * @property hasConfigurationFunction Boolean
 * @constructor
 */
@Ignore
internal data class BeanContainer(
    val pkg: String = "",
    val name: String = "",
    val path: String = "",
    val clazz: KClass<*>
) {
    val annotations: List<Annotation> = clazz.annotationsSafe.toList()
//    val isSingleton: Boolean by lazy { clazz.findAnnotationSafe<Singleton>()?.let { true } ?: false }
    val isController: Boolean  by lazy { clazz.findAnnotationSafe<Controller>()?.let { true } ?: false }
    val routeFunctions by lazy { clazz.functionsSafe.filter { it.findAnnotation<RawRoute>()!=null } }
    val alias: String? by lazy { (annotations.find { it is Alias } as? Alias)?.name }
    val isConfigurationClass: Boolean by lazy {
        clazz.findAnnotationSafe<cn.zenliu.ktor.boot.annotations.context.ApplicationConfiguration>() != null && clazz.isSubclassOf(ApplicationConfiguration::class)
    }
    val hasConfigurationFunction: Boolean by lazy {
        clazz.declaredFunctionsSafe.any {
            (it.annotations.find { it is ApplicationConfiguration } != null) && it.parameters.find {
                it.type.isClass(
                    Application::class
                )
            } != null
        }
    }

    fun configurationFunctions(): Set<KFunction<*>> = clazz.declaredFunctions
        .filter {
            (it.annotations.find { it is ApplicationConfiguration } != null) && it.parameters.find {
                it.kind == KParameter.Kind.VALUE && it.type.isClass(
                    Application::class
                )
            } != null
        }.toSet()

}
