package com.example.backend

import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import jakarta.annotation.PostConstruct

@RestController
@RequestMapping("/api/images")
class ImageController {

    private val uploadDir: Path = Paths.get("uploads/memo_images")

    @PostConstruct
    fun init() {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir)
        }
    }

    @PostMapping("/upload")
    fun uploadImage(@RequestParam("file") file: MultipartFile, @RequestParam("filename") filename: String): ResponseEntity<Void> {
        return try {
            if (file.isEmpty) {
                return ResponseEntity.badRequest().build()
            }
            
            // Prevent path traversal
            if (filename.contains("..") || filename.contains("/")) {
                return ResponseEntity.badRequest().build()
            }

            val targetLocation = uploadDir.resolve(filename)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            
            ResponseEntity.ok().build()
        } catch (ex: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }


    @GetMapping("/{filename:.+}")
    fun downloadImage(@PathVariable filename: String): ResponseEntity<Resource> {
        return try {
            // Prevent path traversal
            if (filename.contains("..") || filename.contains("/")) {
                return ResponseEntity.badRequest().build()
            }

            val file: Path = uploadDir.resolve(filename).normalize()
            val resource: Resource = UrlResource(file.toUri())
            
            if (resource.exists() || resource.isReadable) {
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${resource.filename}\"")
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (ex: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }
}
