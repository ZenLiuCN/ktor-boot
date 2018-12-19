package cn.zenliu.ktor.boot.jackson

import cn.zenliu.ktor.boot.reflect.tryKClass
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.joda.time.DateTime
import java.time.Instant

//<editor-fold desc="Jackson">
object JsonMapper {
    private var mapperObj = ObjectMapper()
    var mapper
        get() = this.mapperObj
        set(value) {
            this.mapperObj = initMapper(value)
        }

    @JvmStatic
    fun initMapper(m: ObjectMapper = this.mapper) =
        m.registerKotlinModule().findAndRegisterModules().registerModule(SimpleModule().apply {
            registJavaTimeInstant()
            registJodaDateTime()
        })

    //<editor-fold desc="JacksonExt">
    @JvmStatic
    inline fun <reified T> SimpleModule.registerDeserializer(crossinline deserializer: (p: JsonParser, dctx: DeserializationContext) -> T) =
        this.addDeserializer(T::class.java, object : JsonDeserializer<T>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T = deserializer(p, ctxt)

        })

    @JvmStatic
    inline fun <reified T> SimpleModule.registerSerializer(crossinline serializer: (value: T, gen: JsonGenerator?, serializers: SerializerProvider?) -> Unit) =
        this.addSerializer(T::class.java, object : JsonSerializer<T>() {
            override fun serialize(value: T, gen: JsonGenerator?, serializers: SerializerProvider?) {
                serializer.invoke(value, gen, serializers)
            }
        })

    @JvmStatic
    fun SimpleModule.registJavaTimeInstant() {
        "java.time.Instant".tryKClass?.let {
            registerDeserializer { p, _ ->
                when {
                    p.text.isNullOrEmpty() -> null
                    p.text == "null" -> null
                    else -> Instant.parse(p.text)
                }/*.apply {
                while (!p.isClosed) {
                    p.nextToken()
                }
            }*/

            }
            registerSerializer<Instant?> { value, gen, _ ->
                gen?.writeRawValue(value?.let {
                    "\"$value\""
                }
                    ?: "null")
            }
        }
    }

    @JvmStatic
    fun SimpleModule.registJodaDateTime() {
        "org.joda.time.DateTime".tryKClass?.let {
            registerDeserializer { p, dctx ->
                when {
                    p.text.isNullOrEmpty() -> null
                    p.text == "null" -> null
                    else -> DateTime.parse(p.text)
                }
            }
            registerSerializer<DateTime?> { value, gen, _ ->
                gen?.writeRawValue(value?.let {
                    "\"$value\""
                }
                    ?: "null")
            }
        }

    }

    //</editor-fold>
    @JvmStatic
    fun toJsonNode(value: Any?) = mapper.readTree(
        when (value) {
            is String -> value
            is ByteArray -> value.toString()
            else -> mapper.writeValueAsString(value)
        }
    )!!

    @JvmStatic
    fun <T> toClass(value: Any?, clazz: Class<T>) = when (value) {
        is String -> JsonMapper.mapper.readValue(value, clazz)
        is ByteArray -> JsonMapper.mapper.readValue(value, clazz)
        else -> JsonMapper.mapper.readValue(JsonMapper.mapper.writeValueAsString(value), clazz)
    }!!
}


//</editor-fold>
