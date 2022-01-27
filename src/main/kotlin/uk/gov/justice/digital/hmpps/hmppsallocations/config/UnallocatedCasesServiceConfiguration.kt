package uk.gov.justice.digital.hmpps.hmppsallocations.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.hmppsallocations.client.AssessmentApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.CourtReportMapper
import uk.gov.justice.digital.hmpps.hmppsallocations.mapper.GradeMapper
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UnallocatedCasesService

@Configuration
class UnallocatedCasesServiceConfiguration {

  @Bean
  fun unallocatedCasesService(
    unallocatedCasesRepository: UnallocatedCasesRepository,
    @Qualifier("communityApiClient") communityApiClient: CommunityApiClient,
    @Qualifier("hmppsTierApiClient") hmppsTierApiClient: HmppsTierApiClient,
    @Qualifier("assessmentApiClient") assessmentApiClient: AssessmentApiClient,
    gradeMapper: GradeMapper,
    courtReportMapper: CourtReportMapper,
  ): UnallocatedCasesService {
    return UnallocatedCasesService(unallocatedCasesRepository, communityApiClient, hmppsTierApiClient, assessmentApiClient, gradeMapper, courtReportMapper)
  }

  @Bean
  fun unallocatedCasesUserEnhancedService(
    unallocatedCasesRepository: UnallocatedCasesRepository,
    @Qualifier("communityApiClientUserEnhanced") communityApiClient: CommunityApiClient,
    @Qualifier("hmppsTierApiClientUserEnhanced") hmppsTierApiClient: HmppsTierApiClient,
    @Qualifier("assessmentApiClientUserEnhanced") assessmentApiClient: AssessmentApiClient,
    gradeMapper: GradeMapper,
    courtReportMapper: CourtReportMapper,
  ): UnallocatedCasesService {
    return UnallocatedCasesService(unallocatedCasesRepository, communityApiClient, hmppsTierApiClient, assessmentApiClient, gradeMapper, courtReportMapper)
  }
}
