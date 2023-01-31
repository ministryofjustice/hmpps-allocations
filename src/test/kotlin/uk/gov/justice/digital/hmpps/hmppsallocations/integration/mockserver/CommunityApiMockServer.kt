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
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.activeSentencedAndPreConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.convictionNoSentenceResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.convictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.multipleRegistrationResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.ogrsResponse

class CommunityApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {
    lateinit var communityApi: CommunityApiMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    communityApi = CommunityApiMockServer()
  }

  override fun beforeEach(context: ExtensionContext?) {
    communityApi.reset()
  }
  override fun afterAll(context: ExtensionContext?) {
    communityApi.stop()
  }
}
class CommunityApiMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8092
  }

  fun singleActiveConvictionResponse(crn: String) {
    val convictionsRequest =
      HttpRequest.request()
        .withPath("/offenders/crn/$crn/convictions").withQueryStringParameter(Parameter("activeOnly", "true"))

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveConvictionResponse())
    )
  }

  fun activeSentenacedAndPreConvictionResponse(crn: String) {
    val convictionsRequest =
      HttpRequest.request()
        .withPath("/offenders/crn/$crn/convictions").withQueryStringParameter(Parameter("activeOnly", "true"))

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(activeSentencedAndPreConvictionResponse())
    )
  }

  fun unallocatedConvictionResponse(crn: String, convictionId: Long, staffCode: String = "STFFCDEU") {
    val convictionsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionResponse(staffCode))
    )
  }

  fun allocatedConvictionResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(convictionResponse("STFFCDE"))
    )
  }

  fun convictionWithNoSentenceResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(convictionNoSentenceResponse("STFFCDEU"))
    )
  }

  fun inactiveConvictionResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(
        uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.inactiveConvictionResponse("STFFCDEU")
      )
    )
  }

  fun notFoundConvictionResponse(crn: String, convictionId: Long) {
    val convictionsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/convictions/$convictionId")

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withStatusCode(HttpStatus.NOT_FOUND.value())
    )
  }

  fun notFoundAllConvictionResponse(crn: String) {
    val convictionsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/convictions")

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withStatusCode(HttpStatus.NOT_FOUND.value())
    )
  }

  fun notFoundActiveConvictionsResponse(crn: String) {
    val convictionsRequest =
      HttpRequest.request()
        .withPath("/offenders/crn/$crn/convictions").withQueryStringParameter(Parameter("activeOnly", "true"))

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withStatusCode(HttpStatus.NOT_FOUND.value())
    )
  }

  fun singleActiveConvictionResponseForAllConvictions(crn: String) {
    val convictionsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/convictions")

    CommunityApiExtension.communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveConvictionResponse())
    )
  }

  fun singleActiveInductionResponse(crn: String) {
    val inductionRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/contact-summary/inductions")

    CommunityApiExtension.communityApi.`when`(inductionRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveInductionResponse())
    )
  }

  fun noActiveInductionResponse(crn: String) {
    val inductionRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/contact-summary/inductions")

    CommunityApiExtension.communityApi.`when`(inductionRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody("[]")
    )
  }

  fun offenderDetailsResponse(crn: String) {
    val summaryRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/all")

    CommunityApiExtension.communityApi.`when`(summaryRequest, Times.exactly(1)).respond(
      HttpResponse.response()
        .withContentType(MediaType.APPLICATION_JSON).withBody(uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderDetailsResponse())
    )
  }

  fun offenderDetailsForbiddenResponse(crn: String) {
    val summaryRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/all")

    CommunityApiExtension.communityApi.`when`(summaryRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withStatusCode(403)
    )
  }

  fun getRegistrationsFromDelius(crn: String) {
    val registrationsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/registrations")

    CommunityApiExtension.communityApi.`when`(registrationsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(multipleRegistrationResponse())
    )
  }

  fun noRegistrationsFromDelius(crn: String) {
    val registrationsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/registrations")

    CommunityApiExtension.communityApi.`when`(registrationsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody("{}")
    )
  }

  fun getOgrsForCrn(crn: String) {
    val ogrsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/assessments")

    CommunityApiExtension.communityApi.`when`(ogrsRequest, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(ogrsResponse())
    )
  }
}
