/* Spotify Web API, Kotlin Wrapper; MIT License, 2017-2022; Original author: Adam Ratzman */
package com.adamratzman.spotify.utils

import com.soywiz.korim.format.jpg.JPEG
import korlibs.crypto.encoding.Base64
import korlibs.image.bitmap.Bitmap
import korlibs.io.file.VfsFile
import korlibs.io.file.std.UrlVfs
import korlibs.io.file.std.localVfs


/**
 * Represents an image. Please use convertXToBufferedImage and convertBufferedImageToX methods to read and write [BufferedImage]
 */
public typealias BufferedImage = Bitmap

public fun convertBufferedImageToBase64JpegString(image: BufferedImage): String {
    return Base64.encode(JPEG.encode(image))
}

public suspend fun convertUrlPathToBufferedImage(url: String): BufferedImage {
    return JPEG.decode(UrlVfs(url))
}

public suspend fun convertLocalImagePathToBufferedImage(path: String): BufferedImage {
    return JPEG.decode(localVfs(path))
}

public suspend fun convertFileToBufferedImage(file: VfsFile): BufferedImage = JPEG.decode(file.readBytes())
