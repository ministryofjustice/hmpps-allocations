package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository

@Service
class UnallocatedCasesService(
  private val unallocatedCasesRepository: UnallocatedCasesRepository
) {

  fun getAll(): List<UnallocatedCase> {
    return unallocatedCasesRepository.findAll().map {
      UnallocatedCase.from(it)
    }
  }

  fun getCase(crn: String): UnallocatedCase? =
    unallocatedCasesRepository.findByCrn(crn)?.let {
      log.info("Found unallocated case for $crn")
      UnallocatedCase.from(it)
    }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
