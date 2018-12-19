package cn.zenliu.ktor.boot.context


import cn.zenliu.ktor.boot.annotations.context.Configuration
import cn.zenliu.ktor.boot.jackson.JsonMapper
import cn.zenliu.ktor.boot.reflect.classExists
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson

//<editor-fold desc="Default">
@Configuration
class DefaultConfiguration : cn.zenliu.ktor.boot.context.Configuration {
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
//</editor-fold>
