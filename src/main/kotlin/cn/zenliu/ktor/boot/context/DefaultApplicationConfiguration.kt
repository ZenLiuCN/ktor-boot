package cn.zenliu.ktor.boot.context


import cn.zenliu.ktor.boot.annotations.context.ApplicationConfiguration
import cn.zenliu.ktor.boot.jackson.JsonMapper
import cn.zenliu.ktor.boot.reflect.classExists
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson

/**
 * Default Application Configuration
 */
@ApplicationConfiguration
class DefaultApplicationConfiguration : cn.zenliu.ktor.boot.context.ApplicationConfiguration {
    override fun applicationConfiguration(app: Application) {
        app.log.info("default configuration running")
        app.apply {
            if ("io.ktor.features.ContentNegotiation".classExists &&
                "com.fasterxml.jackson.databind.ObjectMapper".classExists
            ) {
                app.log.info("default install contentNegotiation of fastxml jackson")
                install(ContentNegotiation) {
                    jackson {
                        JsonMapper.mapper = this
                    }
                }
            }
        }
    }

}

