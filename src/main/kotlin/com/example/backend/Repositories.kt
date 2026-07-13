package com.example.backend

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<AppUser, String>

@Repository
interface RichMemoRepository : JpaRepository<RichMemo, String> {
    fun findByUserId(userId: String): List<RichMemo>
}

@Repository
interface LocationMemoRepository : JpaRepository<LocationMemo, Long> {
    fun findByUserId(userId: String): List<LocationMemo>
}

@Repository
interface StepDataRepository : JpaRepository<StepData, Long> {
    fun findByUserId(userId: String): List<StepData>
    fun findByUserIdAndDate(userId: String, date: String): StepData?
}

@Repository
interface AlarmRepository : JpaRepository<Alarm, String> {
    fun findByUserId(userId: String): List<Alarm>
}

@Repository
interface MedicationItemRepository : JpaRepository<MedicationItem, String> {
    fun findByUserId(userId: String): List<MedicationItem>
}

@Repository
interface MedicationLogRepository : JpaRepository<MedicationLog, Long> {
    fun findByUserId(userId: String): List<MedicationLog>
}

@Repository
interface HospitalVisitRepository : JpaRepository<HospitalVisit, String> {
    fun findByUserId(userId: String): List<HospitalVisit>
}
