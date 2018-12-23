/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

@file:Ignore

package cn.zenliu.ktor.boot.reflect


import cn.zenliu.ktor.boot.annotations.context.Controller
import cn.zenliu.ktor.boot.annotations.context.Ignore
import cn.zenliu.ktor.boot.annotations.context.Properties
import cn.zenliu.ktor.boot.annotations.routing.Interceptor
import cn.zenliu.ktor.boot.annotations.routing.RawRoute
import cn.zenliu.ktor.boot.annotations.routing.RequestMapping
import cn.zenliu.ktor.boot.context.InterceptorContext
import io.ktor.routing.Routing
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.kotlinFunction


//<editor-fold desc="KClassExt">

val String.tryKClass: KClass<*>?
    inline get() = try {
        Thread.currentThread().contextClassLoader.loadClass(this).kotlin
    } catch (e: ClassNotFoundException) {
        null
    }

val String.classExists: Boolean
    inline get() = try {
        Thread.currentThread().contextClassLoader.loadClass(this)
        true
    } catch (e: ClassNotFoundException) {
        false
    }

fun KType.isClass(cls: KClass<*>): Boolean = this.classifier == cls
inline fun <reified T> KType.isClass(): Boolean = this.classifier == T::class

val KType.isTypeString: Boolean inline get() = this.isClass(String::class)|| this.isClass(java.lang.String::class)
val KType.isTypeInt: Boolean inline get() = this.isClass(Int::class) || this.isClass(java.lang.Integer::class)
val KType.isTypeLong: Boolean inline get() = this.isClass(Long::class) || this.isClass(java.lang.Long::class)
val KType.isTypeByte: Boolean inline get() = this.isClass(Byte::class) || this.isClass(java.lang.Byte::class)
val KType.isTypeShort: Boolean inline get() = this.isClass(Short::class) || this.isClass(java.lang.Short::class)
val KType.isTypeChar: Boolean inline get() = this.isClass(Char::class) || this.isClass(java.lang.Character::class)
val KType.isTypeBoolean: Boolean inline get() = this.isClass(Boolean::class) || this.isClass(java.lang.Boolean::class)
val KType.isTypeFloat: Boolean inline get() = this.isClass(Float::class) || this.isClass(java.lang.Float::class)
val KType.isTypeDouble: Boolean inline get() = this.isClass(Double::class) || this.isClass(java.lang.Double::class)
val KType.isTypeByteArray: Boolean inline get() = this.isClass(ByteArray::class)
//val KType.isTypeUInt: Boolean get() = this.isClass(UInt::class)

val KType.kClass: KClass<*>?
    inline get() = when {
        isTypeString -> String::class
        isTypeInt -> Int::class
        isTypeLong -> Long::class
        isTypeByte -> Byte::class
        isTypeShort -> Short::class
        isTypeChar -> Char::class
        isTypeBoolean -> Boolean::class
        isTypeFloat -> Float::class
        isTypeDouble -> Double::class
        isTypeByteArray -> ByteArray::class
        else -> this.javaType.typeName.tryKClass
    }

val KType.simpleDefaultValue: Any?
    inline get() = when {
        isTypeString -> ""
        isTypeInt -> 0
        isTypeLong -> 0L
        isTypeByte -> 0.toByte()
        isTypeShort -> 0.toShort()
        isTypeChar -> 0.toChar()
        isTypeBoolean -> false
        isTypeFloat -> 0.toFloat()
        isTypeDouble -> 0.toDouble()
        isTypeByteArray -> "".toByteArray()
        else -> null
    }

inline fun <reified T : Annotation> KClass<*>.findAnnotationSafe() = try {
    this.annotations.firstOrNull { it is T } as T?
} catch (e: UnsupportedOperationException) {
    this.java.annotations.firstOrNull { it is T } as T?
}


val KClass<*>.declaredFunctionsSafe
    inline get() = try {
        this.declaredFunctions
    } catch (e: UnsupportedOperationException) {
        this.java.methods.map { it.kotlinFunction }.filterNotNull()
    }


val KClass<*>.functionsSafe
    inline get() = try {
        this.functions
    } catch (e: UnsupportedOperationException) {
        this.java.methods.map { it.kotlinFunction }.filterNotNull()
    }

val KClass<*>.annotationsSafe
    inline get() = try {
        this.annotations
    } catch (e: UnsupportedOperationException) {
        this.java.annotations.filterNotNull()
    }
val KClass<*>.isPropertiesClass: Boolean
    inline get() = this.findAnnotationSafe<Properties>() != null && this.isData

//</editor-fold>

val KFunction<*>.isInterceptor
    get() = {
        this.findAnnotation<Interceptor>() != null &&
                this.parameters.size == 2 &&
                this.returnType.isClass<Unit>() &&
                (this.parameters[0].kind == KParameter.Kind.INSTANCE || this.parameters[0].kind == KParameter.Kind.EXTENSION_RECEIVER) &&
                this.parameters[1].kind == KParameter.Kind.VALUE &&
                this.parameters[1].type.isClass<InterceptorContext>()
    }.invoke()

val KFunction<*>.isRawRoute
    get() = {
        this.findAnnotation<RawRoute>() != null &&
                this.parameters.size == 2 &&
                this.returnType.isClass<Unit>() &&
                (this.parameters[0].kind == KParameter.Kind.INSTANCE || this.parameters[0].kind == KParameter.Kind.EXTENSION_RECEIVER) &&
                this.parameters[1].kind == KParameter.Kind.VALUE &&
                this.parameters[1].type.isClass<Routing>()
    }.invoke()

val KFunction<*>.isRequestHandler
    get() = {
        this.findAnnotation<RequestMapping>() != null &&
                this.isSuspend &&
                !this.returnType.isClass<Unit>()
    }.invoke()

val KClass<*>.isController
    get() = {
        this.findAnnotation<Controller>() != null &&
                this.declaredFunctionsSafe.find { it.isRequestHandler } != null
    }.invoke()
