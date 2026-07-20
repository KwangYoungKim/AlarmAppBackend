package com.example.backend

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "app_users")
data class AppUser(
    @Id
    val id: String, // UUID
    
    var nickname: String,
    
    var pin: String // 4-digit pin for restore
)

@Entity
data class RichMemo(
    @Id
    val id: String,
    var userId: String? = null,
    var title: String,
    @Column(columnDefinition = "TEXT")
    var contentJson: String,
    var lastModified: Long,
    var createdAt: Long,
    @Column(columnDefinition = "boolean default false")
    var pinned: Boolean = false
)

@Entity
data class LocationMemo(
    @Id
    val id: Long, // timestamp
    var userId: String? = null,
    val date: String,
    val lat: Double,
    val lng: Double,
    val memo: String,
    val time: String
)

@Entity
data class StepData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    var userId: String? = null,
    val date: String,
    val steps: Int,
    val goal: Int
)

@Entity
data class Alarm(
    @Id
    val id: String,
    var userId: String? = null,
    val intervalMinutes: Int,
    val nextTriggerTimeMillis: Long,
    val title: String = "",
    val ringtoneUri: String = "",
    val skipNext: Boolean = false,
    val isRunning: Boolean = true,
    val remainingTimeMillis: Long = 0L
)

@Entity
data class MedicationItem(
    @Id
    val id: String,
    var userId: String? = null,
    val name: String,
    @ElementCollection(fetch = FetchType.EAGER)
    val times: List<String>,
    val createdAt: String
)

@Entity
data class MedicationLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    var userId: String? = null,
    val date: String,
    val pillId: String,
    val time: String,
    val isTaken: Boolean
)

@Entity
data class HospitalVisit(
    @Id
    val id: String,
    var userId: String? = null,
    val date: String,
    val visitTime: String,
    val morningAlarmTime: String,
    val note: String
)

@Entity
data class DailyPath(
    @Id
    val id: String, // userId_YYYY-MM-DD
    var userId: String? = null,
    val date: String,
    @Column(columnDefinition = "TEXT")
    val pathJson: String
)
