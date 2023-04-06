package uk.gov.justice.digital.hmpps.hmppsallocations.integration.unallocatedcases

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.CommunityApiExtension
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.TierApiExtension
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.WorkforceAllocationsToDeliusApiExtension
import java.io.File

class TriggerProcessingCrnTests : IntegrationTestBase() {

  @Test
  fun `trigger a processing from upload`() {
    val crn = "J678910"
    CommunityApiExtension.communityApi.getUserAccessForCrn(crn)
    WorkforceAllocationsToDeliusApiExtension.workforceAllocationsToDelius.unallocatedEventsResponse(crn)
    TierApiExtension.hmppsTier.tierCalculationResponse(crn)

    webTestClient.post()
      .uri("/crn/reprocess")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .body(generateMultipartBody(crn))
      .exchange()
      .expectStatus()
      .isOk

    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()
    Assertions.assertEquals(crn, case.crn)
  }

  @Test
  fun `remove any special characters in lines from upload`() {
    val crn = "J678910"
    val crnWithSpeechMarks = "\"$crn\""
    CommunityApiExtension.communityApi.getUserAccessForCrn(crn)
    WorkforceAllocationsToDeliusApiExtension.workforceAllocationsToDelius.unallocatedEventsResponse(crn)
    TierApiExtension.hmppsTier.tierCalculationResponse(crn)

    webTestClient.post()
      .uri("/crn/reprocess")
      .contentType(MediaType.MULTIPART_FORM_DATA)
      .body(generateMultipartBody(crnWithSpeechMarks))
      .exchange()
      .expectStatus()
      .isOk

    await untilCallTo { repository.count() } matches { it!! > 0 }

    val case = repository.findAll().first()
    Assertions.assertEquals(crn, case.crn)
  }

  private fun generateMultipartBody(crn1: String, crn2: String = ""): BodyInserters.MultipartInserter {
    val cases = listOf(crn1, crn2)
    val csvFile = generateCsv(cases)
    val multipartBodyBuilder = MultipartBodyBuilder()
    multipartBodyBuilder.part("file", FileSystemResource(csvFile))
    return BodyInserters.fromMultipartData(multipartBodyBuilder.build())
  }

  fun generateCsv(unallocatedCases: List<String>): File {
    val tempFile = kotlin.io.path.createTempFile().toFile()
    tempFile.printWriter().use { out ->
      unallocatedCases.forEach {
        out.println(it)
      }
    }
    return tempFile
  }
}
