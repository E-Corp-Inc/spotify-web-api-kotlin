/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2022; Original author: Adam Ratzman */
package com.soywiz.korim.format.jpg

import korlibs.image.format.ImageData
import korlibs.image.format.ImageDecodingProps
import korlibs.image.format.ImageEncodingProps
import korlibs.image.format.ImageFormat
import korlibs.image.format.ImageFrame
import korlibs.image.format.ImageInfo
import korlibs.io.stream.SyncStream
import korlibs.io.stream.readAll
import korlibs.io.stream.writeBytes


public object JPEG : ImageFormat("jpg", "jpeg") {
    override fun decodeHeader(s: SyncStream, props: ImageDecodingProps): ImageInfo? = try {
        val info = JPEGDecoder.decodeInfo(s.readAll())
        ImageInfo().apply {
            this.width = info.width
            this.height = info.height
            this.bitsPerPixel = 24
        }
    } catch (e: Throwable) {
        null
    }

    override fun readImage(s: SyncStream, props: ImageDecodingProps): ImageData {
        @Suppress("DEPRECATION")
        return ImageData(listOf(ImageFrame(JPEGDecoder.decode(s.readAll()))))
    }

    override fun writeImage(image: ImageData, s: SyncStream, props: ImageEncodingProps) {
        s.writeBytes(JPEGEncoder.encode(image.mainBitmap.toBMP32(), quality = (props.quality * 100).toInt()))
    }
}
