package com.example.backend

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class DatabaseBackupService(
    private val userRepository: UserRepository,
    private val richMemoRepository: RichMemoRepository,
    private val locationMemoRepository: LocationMemoRepository,
    private val stepDataRepository: StepDataRepository,
    private val hospitalVisitRepository: HospitalVisitRepository,
    private val alarmRepository: AlarmRepository,
    private val medicationItemRepository: MedicationItemRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(DatabaseBackupService::class.java)
    private val backupDir = Paths.get("db_backups")

    @Scheduled(cron = "0 0 4 * * ?") // Run at 4 AM every day
    fun backupDatabase() {
        logger.info("Starting automated database backup...")
        try {
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir)
            }

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val backupFile = backupDir.resolve("backup_$timestamp.json")

            val backupData = mapOf(
                "users" to userRepository.findAll(),
                "richMemos" to richMemoRepository.findAll(),
                "locationMemos" to locationMemoRepository.findAll(),
                "stepData" to stepDataRepository.findAll(),
                "hospitalVisits" to hospitalVisitRepository.findAll(),
                "alarms" to alarmRepository.findAll(),
                "medicationItems" to medicationItemRepository.findAll(),
                "medicationLogs" to medicationLogRepository.findAll()
            )

            val jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(backupData)
            Files.writeString(backupFile, jsonContent)

            logger.info("Database backup completed successfully: ${backupFile.fileName}")

            // Retention Policy: Delete backups older than 7 days
            cleanupOldBackups()

        } catch (e: Exception) {
            logger.error("Failed to backup database", e)
        }
    }

    private fun cleanupOldBackups() {
        try {
            val now = LocalDateTime.now()
            Files.list(backupDir).use { stream ->
                stream.forEach { filePath ->
                    if (Files.isRegularFile(filePath) && filePath.fileName.toString().startsWith("backup_")) {
                        val lastModifiedTime = Files.getLastModifiedTime(filePath).toInstant()
                        val fileTime = LocalDateTime.ofInstant(lastModifiedTime, java.time.ZoneId.systemDefault())
                        if (ChronoUnit.DAYS.between(fileTime, now) > 7) {
                            Files.delete(filePath)
                            logger.info("Deleted old database backup: ${filePath.fileName}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to clean up old database backups", e)
        }
    }
}
