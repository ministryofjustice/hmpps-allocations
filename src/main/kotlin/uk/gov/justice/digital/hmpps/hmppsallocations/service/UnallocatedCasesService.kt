package uk.gov.justice.digital.hmpps.hmppsallocations.service

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
}
