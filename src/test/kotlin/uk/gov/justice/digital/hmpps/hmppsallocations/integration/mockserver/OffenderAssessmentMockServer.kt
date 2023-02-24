package uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.mockserver.model.Parameter
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.OffenderAssessmentApiExtension.Companion.offenderAssessmentApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessment.assessmentNotFoundResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessment.assessmentResponse

class OffenderAssessmentApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {

    lateinit var offenderAssessmentApi: OffenderAssessmentMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    offenderAssessmentApi = OffenderAssessmentMockServer()
  }

  override fun beforeEach(context: ExtensionContext?) {
    offenderAssessmentApi.reset()
  }
  override fun afterAll(context: ExtensionContext?) {
    offenderAssessmentApi.stop()
  }
}
class OffenderAssessmentMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8072
  }

  fun getAssessmentsForCrn(crn: String) {
    val assessmentRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/assessments/summary")
        .withQueryStringParameter(Parameter("assessmentStatus", "COMPLETE"))

    offenderAssessmentApi.`when`(assessmentRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(assessmentResponse())
    )
  }

  fun notFoundAssessmentForCrn(crn: String) {
    val assessmentRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/assessments/summary")
        .withQueryStringParameter(Parameter("assessmentStatus", "COMPLETE"))
    offenderAssessmentApi.`when`(assessmentRequest, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatus.NOT_FOUND.value()).withContentType(MediaType.APPLICATION_JSON)
        .withBody(assessmentNotFoundResponse(crn))
    )
  }
}
