package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableCaching
@EnableScheduling
class CacheConfiguration {

  @Bean
  fun cacheManager(): CacheManager {
    return ConcurrentMapCacheManager(INDUCTION_APPOINTMENT_CACHE_NAME)
  }

  @CacheEvict(allEntries = true, cacheNames = [INDUCTION_APPOINTMENT_CACHE_NAME])
  @Scheduled(cron = "\${application.cache.induction.cron}")
  fun cacheEvict() {
    log.info("Evicting cache $INDUCTION_APPOINTMENT_CACHE_NAME")
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val INDUCTION_APPOINTMENT_CACHE_NAME: String = "inductionAppointment"
  }
}
