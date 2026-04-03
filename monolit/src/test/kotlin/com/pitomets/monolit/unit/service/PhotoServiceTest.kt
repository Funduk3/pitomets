package com.pitomets.monolit.unit.service

import com.pitomets.monolit.service.PhotoService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

class PhotoServiceTest {

    private val photoService = TestablePhotoService()

    @Test
    fun `validateImage throws when file is empty`() {
        val file = MockMultipartFile("file", "empty.jpg", "image/jpeg", ByteArray(0))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            photoService.validate(file)
        }

        assertEquals("File is empty", ex.message)
    }

    @Test
    fun `validateImage throws when content type is unsupported`() {
        val file = MockMultipartFile("file", "doc.gif", "image/gif", "123".toByteArray())

        val ex = assertThrows(IllegalArgumentException::class.java) {
            photoService.validate(file)
        }

        assertEquals(
            "Unsupported content type: image/gif. Allowed: image/jpeg, image/png, image/webp",
            ex.message
        )
    }

    @Test
    fun `validateImage throws when file is bigger than max size`() {
        val tooBig = ByteArray(PhotoService.MAX_FILE_SIZE.toInt() + 1)
        val file = MockMultipartFile("file", "huge.png", "image/png", tooBig)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            photoService.validate(file)
        }

        assertEquals("File is too large. Maximum size: 5MB", ex.message)
    }

    @Test
    fun `validateImage passes for valid file`() {
        val file = MockMultipartFile("file", "ok.webp", "image/webp", "abc".toByteArray())

        photoService.validate(file)
    }

    @Test
    fun `extractExtension returns file extension`() {
        assertEquals("png", photoService.extensionOf("photo.png"))
        assertEquals("gz", photoService.extensionOf("archive.tar.gz"))
    }

    @Test
    fun `extractExtension returns jpg when filename has no extension or null`() {
        assertEquals("jpg", photoService.extensionOf("filename"))
        assertEquals("jpg", photoService.extensionOf(null))
    }

    private class TestablePhotoService : PhotoService() {
        fun validate(file: MockMultipartFile) = validateImage(file)
        fun extensionOf(filename: String?) = extractExtension(filename)
    }
}
