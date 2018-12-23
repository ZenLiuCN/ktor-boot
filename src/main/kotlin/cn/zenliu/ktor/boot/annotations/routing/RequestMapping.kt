/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.routing

import cn.zenliu.ktor.boot.annotations.context.Ignore
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod


/**
 * mark of an routing handler function
 * @property path String
 * @property method HTTPMETHOD
 * @constructor
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@MustBeDocumented
@Ignore
annotation class RequestMapping(val path: String, val method: METHOD = METHOD.GET, val accept: CONTENT = CONTENT.NULL) {
    enum class METHOD(val value:String) {
        GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE"), PATCH("PATCH"), HEAD("HEAD"), OPTIONS("OPTIONS");

        val toHttpMethod
            get() = HttpMethod.parse(this.value)
        companion object {
            fun HttpMethod.toMETHOD()=METHOD.values().find { it.value==this.value }
        }
    }

    enum class CONTENT(val type: String, val subType: String) {
        NULL("", ""),
        ApplicationAny("application", "*"),
        ApplicationAtom("application", "atom+xml"),
        ApplicationJson("application", "json"),
        ApplicationJavaScript("application", "javascript"),
        ApplicationOctetStream("application", "octet-stream"),
        ApplicationFontWoff("application", "font-woff"),
        ApplicationRss("application", "rss+xml"),
        ApplicationXml("application", "xml"),
        ApplicationXmlDtd("application", "xml-dtd"),
        ApplicationZip("application", "zip"),
        ApplicationGZip("application", "gzip"),
        ApplicationFormUrlEncoded("application", "x-www-form-urlencoded"),

        AudioAny("audio", "*"),
        AudioMP4("audio", "mp4"),
        AudioMPEG("audio", "mpeg"),
        AudioOGG("audio", "ogg"),

        ImageAny("image", "*"),
        ImageGIF("image", "gif"),
        ImageJPEG("image", "jpeg"),
        ImagePNG("image", "png"),
        ImageSVG("image", "svg+xml"),
        ImageXIcon("image", "x-icon"),

        MessageAny("message", "*"),
        MessageHttp("message", "http"),

        MultiPartAny("multipart", "*"),
        MultiPartMixed("multipart", "mixed"),
        MultiPartAlternative("multipart", "alternative"),
        MultiPartRelated("multipart", "related"),
        MultiPartFormData("multipart", "form-data"),
        MultiPartSigned("multipart", "signed"),
        MultiPartEncrypted("multipart", "encrypted"),
        MultiPartByteRanges("multipart", "byteranges"),

        TextAny("text", "*"),
        TextPlain("text", "plain"),
        TextCSS("text", "css"),
        TextCSV("text", "csv"),
        TextHtml("text", "html"),
        TextJavaScript("text", "javascript"),
        TextVCard("text", "vcard"),
        TextXml("text", "xml"),

        VideoAny("video", "*"),
        VideoMPEG("video", "mpeg"),
        VideoMP4("video", "mp4"),
        VideoOGG("video", "ogg"),
        VideoQuickTime("video", "quicktime");

        fun toContentType() = ContentType(type, subType)
    }
}
