package uk.gov.justice.digital.hmpps.hmppsallocations

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsAllocations

fun main(args: Array<String>) {
  runApplication<HmppsAllocations>(*args)
}
