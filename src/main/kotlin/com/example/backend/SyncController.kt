package com.example.backend

import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional

@RestController
@RequestMapping("/api")
class SyncController(
    private val userRepository: UserRepository,
    private val richMemoRepository: RichMemoRepository,
    private val locationMemoRepository: LocationMemoRepository,
    private val stepDataRepository: StepDataRepository,
    private val alarmRepository: AlarmRepository,
    private val medicationItemRepository: MedicationItemRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val hospitalVisitRepository: HospitalVisitRepository,
    private val dailyPathRepository: DailyPathRepository
) {

    // --- Auth / User ---
    @PostMapping("/users/register")
    fun register(@RequestBody user: AppUser): ResponseEntity<String> {
        userRepository.save(user)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/users/login")
    fun login(@RequestBody request: Map<String, String>): ResponseEntity<AppUser> {
        val nickname = request["nickname"] ?: return ResponseEntity.badRequest().build()
        val pin = request["pin"] ?: return ResponseEntity.badRequest().build()
        
        val user = userRepository.findAll().find { it.nickname == nickname && it.pin == pin }
        return if (user != null) ResponseEntity.ok(user) else ResponseEntity.status(401).build()
    }

    // --- Rich Memos (Text/Image) ---
    @PostMapping("/sync/{userId}/richmemos")
    fun syncRichMemos(@PathVariable userId: String, @RequestBody memos: List<RichMemo>): ResponseEntity<Void> {
        val existingMap = richMemoRepository.findByUserId(userId).associateBy { it.id }
        
        memos.forEach { incoming ->
            val existing = existingMap[incoming.id]
            if (existing == null || incoming.lastModified > existing.lastModified) {
                richMemoRepository.save(incoming.copy(userId = userId))
            }
        }
        return ResponseEntity.ok().build()
    }

    @GetMapping("/sync/{userId}/richmemos")
    fun getRichMemos(@PathVariable userId: String): ResponseEntity<List<RichMemo>> {
        return ResponseEntity.ok(richMemoRepository.findByUserId(userId))
    }

    @DeleteMapping("/sync/{userId}/richmemos/{memoId}")
    fun deleteRichMemo(@PathVariable userId: String, @PathVariable memoId: String): ResponseEntity<String> {
        val memo = richMemoRepository.findById(memoId).orElse(null)
        if (memo != null && memo.userId == userId) {
            richMemoRepository.delete(memo)
        }
        return ResponseEntity.ok().build()
    }

    // --- Location Memos (Walk Map) ---
    @PostMapping("/sync/{userId}/memos")
    fun syncMemos(@PathVariable userId: String, @RequestBody memos: List<LocationMemo>): ResponseEntity<Void> {
        val existing = locationMemoRepository.findByUserId(userId)
        val datesInRequest = memos.map { it.date }.toSet()
        if (datesInRequest.isNotEmpty()) {
            val toDelete = existing.filter { it.date in datesInRequest }
            locationMemoRepository.deleteAll(toDelete)
            locationMemoRepository.flush()
        }
        locationMemoRepository.saveAll(memos.map { it.copy(userId = userId) })
        return ResponseEntity.ok().build()
    }

    @GetMapping("/sync/{userId}/memos")
    fun getMemos(@PathVariable userId: String): ResponseEntity<List<LocationMemo>> {
        return ResponseEntity.ok(locationMemoRepository.findByUserId(userId))
    }

    // --- Steps ---
    @PostMapping("/sync/{userId}/steps")
    fun syncSteps(@PathVariable userId: String, @RequestBody steps: List<StepData>): ResponseEntity<Void> {
        val existing = stepDataRepository.findByUserId(userId)
        val datesInRequest = steps.map { it.date }.toSet()
        if (datesInRequest.isNotEmpty()) {
            val toDelete = existing.filter { it.date in datesInRequest }
            stepDataRepository.deleteAll(toDelete)
            stepDataRepository.flush()
        }
        stepDataRepository.saveAll(steps.map { it.copy(userId = userId) })
        return ResponseEntity.ok().build()
    }

    @GetMapping("/sync/{userId}/steps")
    fun getSteps(@PathVariable userId: String): ResponseEntity<List<StepData>> {
        return ResponseEntity.ok(stepDataRepository.findByUserId(userId))
    }

    // --- Alarms ---
    @PostMapping("/sync/{userId}/alarms")
    fun syncAlarms(@PathVariable userId: String, @RequestBody alarms: List<Alarm>): ResponseEntity<Void> {
        val existing = alarmRepository.findByUserId(userId)
        alarmRepository.deleteAll(existing)
        alarmRepository.flush()
        alarmRepository.saveAll(alarms.map { it.copy(userId = userId) })
        return ResponseEntity.ok().build()
    }

    @GetMapping("/sync/{userId}/alarms")
    fun getAlarms(@PathVariable userId: String): ResponseEntity<List<Alarm>> {
        return ResponseEntity.ok(alarmRepository.findByUserId(userId))
    }

    // --- Medications ---
    @PostMapping("/sync/{userId}/medications")
    fun syncMedications(
        @PathVariable userId: String,
        @RequestBody data: Map<String, Any>
    ): ResponseEntity<Void> {
        // We expect items and logs
        val itemsList = data["items"] as? List<Map<String, Any>> ?: emptyList()
        val logsList = data["logs"] as? List<Map<String, Any>> ?: emptyList()

        val items = itemsList.map { 
            MedicationItem(
                it["id"].toString(), userId, it["name"].toString(), 
                it["times"] as List<String>, it["createdAt"].toString()
            ) 
        }
        val logs = logsList.map {
            MedicationLog(
                0, userId, it["date"].toString(), it["pillId"].toString(),
                it["time"].toString(), it["isTaken"] as Boolean
            )
        }

        medicationItemRepository.deleteAll(medicationItemRepository.findByUserId(userId))
        medicationItemRepository.flush()
        medicationItemRepository.saveAll(items)

        val datesInRequest = logs.map { it.date }.toSet()
        if (datesInRequest.isNotEmpty()) {
            val existingLogs = medicationLogRepository.findByUserId(userId)
            val toDelete = existingLogs.filter { it.date in datesInRequest }
            medicationLogRepository.deleteAll(toDelete)
            medicationLogRepository.flush()
        }
        medicationLogRepository.saveAll(logs)

        return ResponseEntity.ok().build()
    }

    @Transactional
    @GetMapping("/sync/{userId}/medications")
    fun getMedications(@PathVariable userId: String): ResponseEntity<Map<String, Any>> {
        val items = medicationItemRepository.findByUserId(userId)
        val logs = medicationLogRepository.findByUserId(userId)
        return ResponseEntity.ok(mapOf("items" to items, "logs" to logs))
    }

    // --- Hospitals ---
    @PostMapping("/sync/{userId}/hospitals")
    fun syncHospitals(@PathVariable userId: String, @RequestBody visits: List<HospitalVisit>): ResponseEntity<Void> {
        val existing = hospitalVisitRepository.findByUserId(userId)
        hospitalVisitRepository.deleteAll(existing)
        hospitalVisitRepository.flush()
        hospitalVisitRepository.saveAll(visits.map { it.copy(userId = userId) })
        return ResponseEntity.ok().build()
    }

    @GetMapping("/sync/{userId}/hospitals")
    fun getHospitals(@PathVariable userId: String): ResponseEntity<List<HospitalVisit>> {
        return ResponseEntity.ok(hospitalVisitRepository.findByUserId(userId))
    }

    // --- Legacy Image Upload Support ---
    @PostMapping("/upload/{username}")
    fun uploadImageFrontend(@PathVariable username: String, @org.springframework.web.bind.annotation.RequestParam("file") file: org.springframework.web.multipart.MultipartFile): ResponseEntity<Void> {
        return try {
            if (file.isEmpty) return ResponseEntity.badRequest().build()
            val filename = file.originalFilename ?: return ResponseEntity.badRequest().build()
            if (filename.contains("..") || filename.contains("/")) return ResponseEntity.badRequest().build()
            
            val uploadDir = java.nio.file.Paths.get("uploads/memo_images")
            if (!java.nio.file.Files.exists(uploadDir)) {
                java.nio.file.Files.createDirectories(uploadDir)
            }
            
            val targetLocation = uploadDir.resolve(filename)
            java.nio.file.Files.copy(file.inputStream, targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            
            ResponseEntity.ok().build()
        } catch (ex: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }

    // --- Daily Paths ---
    @PostMapping("/sync/{userId}/paths")
    fun syncPaths(@PathVariable userId: String, @RequestBody paths: List<DailyPath>): ResponseEntity<Void> {
        val existing = dailyPathRepository.findByUserId(userId)
        val datesInRequest = paths.map { it.date }.toSet()
        if (datesInRequest.isNotEmpty()) {
            val toDelete = existing.filter { it.date in datesInRequest }
            dailyPathRepository.deleteAll(toDelete)
            dailyPathRepository.flush()
        }
        dailyPathRepository.saveAll(paths.map { it.copy(userId = userId) })
        return ResponseEntity.ok().build()
    }

    @GetMapping("/sync/{userId}/paths")
    fun getPaths(@PathVariable userId: String): ResponseEntity<List<DailyPath>> {
        return ResponseEntity.ok(dailyPathRepository.findByUserId(userId))
    }
}
