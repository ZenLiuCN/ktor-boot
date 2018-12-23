/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.Ignore
import cn.zenliu.ktor.boot.annotations.context.Properties
import cn.zenliu.ktor.boot.jackson.toClass
import cn.zenliu.ktor.boot.reflect.*
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.ktor.config.ApplicationConfigValue
import io.ktor.server.engine.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.io.charsets.Charset
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

/**
 * manage all properties defined in configuration files
 * @see [config](https://github.com/lightbend/config)
 */
@Ignore
@KtorExperimentalAPI
object PropertiesManager {

    private val applicationIdPath = "ktor.application.id"
    private val hostConfigPath = "ktor.deployment.host"
    private val hostPortPath = "ktor.deployment.port"
    private val hostWatchPaths = "ktor.deployment.watch"
    private val hostSslPortPath = "ktor.deployment.sslPort"
    private val hostSslKeyStore = "ktor.security.ssl.keyStore"
    private val hostSslKeyAlias = "ktor.security.ssl.keyAlias"
    private val hostSslKeyStorePassword = "ktor.security.ssl.keyStorePassword"
    private val hostSslPrivateKeyPassword = "ktor.security.ssl.privateKeyPassword"

    private var env:ApplicationEngineEnvironment?=null
    private val properties = mutableMapOf<String, KClass<*>>()
    private var conf: ApplicationConfig? = null
    val config: ApplicationConfig
        get() = if (conf == null) {
            throw Throwable("configuration not init")
        } else {
            conf!!
        }
    val configuration by lazy { ConfigFactory.load() }
    val application: Application
        get() = if (env == null) {
            throw Throwable("application not init")
        } else {
            env!!.application
        }

    fun setConfiguration(args: Array<String>) = commandLineEnvironment(args).apply {
        this@PropertiesManager.env = this
        this@PropertiesManager.conf = this.config
    }
    fun port(default:Int=80)=config.propertyOrNull(hostPortPath)?.getString()?.toInt()?:default
    fun host(default:String="0.0.0.0")=config.propertyOrNull(hostConfigPath)?.getString()?:default
    fun watchPaths(default:List<String> =emptyList())=config.propertyOrNull(hostWatchPaths)?.getList()?:default
    fun getProperties(kClass: KClass<*>)=when{
            kClass.isPropertiesClass->kClass.findAnnotationSafe<Properties>()!!.let { anno->
                kClass.objectInstance?:kClass.constructors.first().let{
                    con->
                    con.callBy(con.parameters.map {
                        it to this.configurate(
                            anno.path.let { if(it.isBlank())kClass.qualifiedName!! else it }
                        ).propertyOrNull( it.name?:"NULL").let{ conf->
                             when{
                                 it.isOptional && conf==null-> ""
                                  conf==null-> null
                                 else-> conf.getOfType(it.type)
                             }
                         }

                    }.filter { !(it.first.isOptional&& it.second==null) }.toMap()
                    )
                }
            }
                else->null
    }
    inline fun <reified T> getProperties()= PropertiesManager.getProperties(T::class) as T?

    fun string(path:String,default:String?=null)=config.propertyOrNull(path)?.getString()?:default
    fun int(path:String,default:Int?=null)=config.propertyOrNull(path)?.getString()?.toIntOrNull()?:default
    fun bool(path:String,default:Boolean?=null)=config.propertyOrNull(path)?.getString()?.toBoolean()?:default
    fun long(path:String,default:Long?=null)=config.propertyOrNull(path)?.getString()?.toLongOrNull()?:default
    fun short(path:String,default:Short?=null)=config.propertyOrNull(path)?.getString()?.toShortOrNull()?:default
    fun float(path:String,default:Float?=null)=config.propertyOrNull(path)?.getString()?.toFloatOrNull()?:default
    fun bigDecimal(path:String,default:BigDecimal?=null)=config.propertyOrNull(path)?.getString()?.toBigDecimalOrNull()?:default
    fun byteArray(path:String,charset:Charset=Charsets.UTF_8,default:ByteArray?=null)=config.propertyOrNull(path)?.getString()?.toByteArray(charset)?:default
    fun byte(path:String,default:Byte?=null)=config.propertyOrNull(path)?.getString()?.toByteOrNull()?:default
    fun bigInteger(path:String,default:BigInteger?=null)=config.propertyOrNull(path)?.getString()?.toBigIntegerOrNull()?:default
    fun double(path:String,default:Double?=null)=config.propertyOrNull(path)?.getString()?.toDoubleOrNull()?:default

    fun raw(path:String)=config.propertyOrNull(path)
    fun configurate(path:String)=config.config(path)
    @JvmStatic
    fun ApplicationConfigValue?.getOfType(type: KType)=if (this==null && type.isMarkedNullable) null else this!!.getString()
    @JvmStatic
    fun ApplicationConfigValue.getOfType(type: KType)=parseParameter(type,this.getString())
    @JvmStatic
    fun parseParameter(type: KType, value: String) = when {
        type.isTypeString->value
        type.isTypeInt -> value.toInt()
        type.isTypeFloat -> value.toFloat()
        type.isTypeLong -> value.toLong()
        type.isTypeDouble -> value.toDouble()
        type.isTypeShort -> value.toShort()
        type.isTypeBoolean -> value.toBoolean()
        type.isTypeChar -> value[0]
        type.isTypeByte -> value.toByte()
        type.isTypeByteArray -> value.toByteArray()
        else -> value.toClass(Class.forName(type.javaType.typeName))
    }
}
