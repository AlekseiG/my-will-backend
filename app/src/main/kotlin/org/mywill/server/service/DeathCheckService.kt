package org.mywill.server.service

import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.mywill.server.repository.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * Фоновый сервис для периодической проверки статуса пользователей.
 * Переводит пользователей в статус "умер" по истечении таймаута после подтверждения смерти доверенными лицами.
 */
@Service
class DeathCheckService(private val userRepository: UserRepository) {

    /**
     * Выполняет проверку всех пользователей, для которых было зафиксировано время последнего подтверждения смерти.
     * Если с момента `deathConfirmedAt` прошло более `deathTimeoutSeconds` секунд, устанавливает флаг `isDead = true`.
     * Запускается автоматически с заданным интервалом.
     */
    @Scheduled(fixedRateString = "\${app.death-check.fixed-rate:60000}", initialDelayString = "\${app.death-check.initial-delay:180000}")
    @SchedulerLock(name = "DeathCheckService_checkDeaths", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    fun checkDeaths() {
        logger.trace { "checkDeaths started" }
        val usersPendingDeath = userRepository.findByDeathConfirmedAtIsNotNullAndIsDeadFalse()
        logger.debug { "usersPendingDeath size: ${usersPendingDeath.size}" }
        usersPendingDeath.forEach { user ->
            val timeoutAt = user.deathConfirmedAt!!.plus(user.deathTimeoutSeconds.seconds)
            logger.debug { "user: ${user.email}, deathConfirmedAt: ${user.deathConfirmedAt}, timeoutSeconds: ${user.deathTimeoutSeconds}, timeoutAt: $timeoutAt, now: $Clock.System.now()" }
            if (Clock.System.now() > timeoutAt) {
                logger.debug { "user ${user.email} is dead" }
                user.isDead = true
                userRepository.save(user)
            }
        }
    }
}
