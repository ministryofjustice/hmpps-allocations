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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.ProbateEstateApiExtension.Companion.hmppsProbateEstate
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.probationestate.regionsAndTeamsResponseBody

class ProbateEstateApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    lateinit var hmppsProbateEstate: ProbateEstateMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    hmppsProbateEstate = ProbateEstateMockServer()
  }
  override fun beforeEach(context: ExtensionContext?) {
    hmppsProbateEstate.reset()
  }
  override fun afterAll(context: ExtensionContext?) {
    hmppsProbateEstate.stop()
  }
}
class ProbateEstateMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8083
  }

  fun regionsAndTeamsSuccessResponse(
    teams: List<Pair<String, String>>,
    regions: List<Pair<String, String>>,
  ): HttpRequest {
    val request = HttpRequest.request()
      .withPath("/regions")
      .withMethod(HttpMethod.POST.name())

    hmppsProbateEstate.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
        .withBody(
          regionsAndTeamsResponseBody(
            regions,
            teams,
          ),
        ),
    )
    return request
  }

  fun regionsAndTeamsFailsWithInternalServerErrorResponse(): HttpRequest {
    val request = HttpRequest.request()
      .withPath("/regions")
      .withMethod(HttpMethod.POST.name())

    hmppsProbateEstate.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
        .withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()),
    )
    return request
  }

  fun regionsAndTeamsFailsWithBadRequestResponse(): HttpRequest {
    val request = HttpRequest.request()
      .withPath("/regions")
      .withMethod(HttpMethod.POST.name())

    hmppsProbateEstate.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON)
        .withStatusCode(HttpStatus.BAD_REQUEST.value()),
    )
    return request
  }
}
