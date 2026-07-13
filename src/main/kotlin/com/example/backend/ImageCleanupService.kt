package com.example.backend

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class ImageCleanupService(
    private val richMemoRepository: RichMemoRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ImageCleanupService::class.java)
    private val uploadDir = Paths.get("uploads/memo_images")

    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시에 실행
    fun cleanupOrphanedImages() {
        logger.info("Starting orphaned image cleanup...")
        if (!Files.exists(uploadDir)) return

        val allMemos = richMemoRepository.findAll()
        val referencedImages = mutableSetOf<String>()

        val typeRef = object : TypeReference<List<Map<String, Any>>>() {}

        for (memo in allMemos) {
            try {
                val blocks = objectMapper.readValue(memo.contentJson, typeRef)
                for (block in blocks) {
                    if (block["type"] == "image") {
                        val content = block["content"] as? String
                        if (content != null && (content.startsWith("web_") || content.endsWith(".jpg") || content.endsWith(".png"))) {
                            val extractedName = content.substringAfterLast("/")
                            referencedImages.add(extractedName)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to parse memo content for memo ID: ${memo.id}", e)
            }
        }

        var backedUpCount = 0
        try {
            val backupDir = Paths.get("uploads/deleted_images_backup")
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir)
            }

            Files.list(uploadDir).use { stream ->
                stream.forEach { filePath ->
                    if (Files.isRegularFile(filePath)) {
                        val fileName = filePath.fileName.toString()
                        if (!referencedImages.contains(fileName)) {
                            try {
                                val destPath = backupDir.resolve(fileName)
                                Files.move(filePath, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                                backedUpCount++
                                logger.info("Backed up orphaned image: $fileName")
                            } catch (e: Exception) {
                                logger.error("Failed to backup orphaned image: $fileName", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to list files in upload directory", e)
        }

        logger.info("Finished orphaned image cleanup. Backed up $backedUpCount images.")
        
        try {
            var permanentlyDeletedCount = 0
            val now = LocalDateTime.now()
            val backupDir = Paths.get("uploads/deleted_images_backup")
            if (Files.exists(backupDir)) {
                Files.list(backupDir).use { stream ->
                    stream.forEach { filePath ->
                        if (Files.isRegularFile(filePath)) {
                            val lastModified = Files.getLastModifiedTime(filePath).toInstant()
                            val fileTime = LocalDateTime.ofInstant(lastModified, java.time.ZoneId.systemDefault())
                            if (ChronoUnit.DAYS.between(fileTime, now) > 30) {
                                Files.delete(filePath)
                                permanentlyDeletedCount++
                                logger.info("Permanently deleted 30+ day old backup image: ${filePath.fileName}")
                            }
                        }
                    }
                }
            }
            if (permanentlyDeletedCount > 0) {
                logger.info("Permanently deleted $permanentlyDeletedCount old backup images.")
            }
        } catch (e: Exception) {
            logger.error("Failed to clean up old backup images", e)
        }
    }
}
