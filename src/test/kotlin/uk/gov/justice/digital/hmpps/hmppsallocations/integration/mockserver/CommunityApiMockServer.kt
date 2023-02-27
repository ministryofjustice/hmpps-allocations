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
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.mockserver.CommunityApiExtension.Companion.communityApi
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.communityapi.deliusUserAccessForbiddenResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.communityapi.deliusUserAccessResponse

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

  fun getUserAccessForCrn(crn: String) {
    val request = HttpRequest.request().withPath("/offenders/crn/$crn/userAccess")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(deliusUserAccessResponse())
    )
  }

  fun getUserAccessForCrnNotFound(crn: String) {
    val request = HttpRequest.request().withPath("/offenders/crn/$crn/userAccess")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withStatusCode(HttpStatus.NOT_FOUND.value())
    )
  }

  fun getUserAccessForCrnForbidden(crn: String) {
    val request = HttpRequest.request().withPath("/offenders/crn/$crn/userAccess")

    communityApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withStatusCode(HttpStatus.FORBIDDEN.value()).withBody(deliusUserAccessForbiddenResponse())
    )
  }
}
