/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.context


import cn.zenliu.ktor.boot.annotations.context.ApplicationConfiguration
import cn.zenliu.ktor.boot.jackson.JsonMapper
import cn.zenliu.ktor.boot.reflect.classExists
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import org.slf4j.LoggerFactory

/**
 * Default Application Configuration
 */
@ApplicationConfiguration
open class DefaultApplicationConfiguration : cn.zenliu.ktor.boot.context.ApplicationConfiguration {
    override fun applicationConfiguration(app: Application) {
        log.info("default configuration running")
        app.apply {
            if ("io.ktor.features.ContentNegotiation".classExists &&
                "com.fasterxml.jackson.databind.ObjectMapper".classExists
            ) {
                log.info("default install contentNegotiation of fastxml jackson")
                install(ContentNegotiation) {
                    jackson {
                        JsonMapper.mapper = this
                    }
                }
            }
        }
    }
    companion object {
        private val log=LoggerFactory.getLogger(DefaultApplicationConfiguration::class.java)
        internal fun getContainer()=DefaultApplicationConfiguration::class.let { cls->
            BeanContainer(
                cls.qualifiedName?.removeSuffix(".DefaultApplicationConfiguration")?:"",
                "DefaultApplicationConfiguration",
                "",
                cls
            )
        }
    }
}

