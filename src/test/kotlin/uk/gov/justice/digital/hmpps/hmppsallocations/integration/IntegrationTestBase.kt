package uk.gov.justice.digital.hmpps.hmppsallocations.integration

import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderSummaryResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveInductionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsUnallocatedCase
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
abstract class IntegrationTestBase {

  var communityApi: ClientAndServer = startClientAndServer(8092)
  var hmppsTier: ClientAndServer = startClientAndServer(8082)
  private var oauthMock: ClientAndServer = startClientAndServer(9090)
  private val gson: Gson = Gson()

  @BeforeEach
  fun `clear queues and database`() {
    repository.deleteAll()
    hmppsDomainSqsClient.purgeQueue(PurgeQueueRequest(hmppsDomainQueue.queueUrl))
    setupOauth()
  }

  private val hmppsDomainQueue by lazy { hmppsQueueService.findByQueueId("hmppsdomainqueue") ?: throw MissingQueueException("HmppsQueue hmppsdomainqueue not found") }
  private val hmppsDomainTopic by lazy { hmppsQueueService.findByTopicId("hmppsdomaintopic") ?: throw MissingQueueException("HmppsTopic hmppsdomaintopic not found") }
  protected val hmppsDomainSqsClient by lazy { hmppsDomainQueue.sqsClient }
  protected val hmppsDomainSnsClient by lazy { hmppsDomainTopic.snsClient }
  protected val hmppsDomainTopicArn by lazy { hmppsDomainTopic.arn }

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  protected lateinit var repository: UnallocatedCasesRepository

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  internal fun HttpHeaders.authToken(roles: List<String> = emptyList()) {
    this.setBearerAuth(
      jwtAuthHelper.createJwt(
        subject = "SOME_USER",
        roles = roles,
        clientId = "some-client"
      )
    )
  }

  protected fun jsonString(any: Any) = objectMapper.writeValueAsString(any) as String

  protected fun unallocatedCaseEvent(
    crn: String,
    status: String
  ) = HmppsEvent(
    "ALLOCATION_REQUIRED", 0, "some event description", "http://dummy.com",
    ZonedDateTime.now().format(
      DateTimeFormatter.ISO_ZONED_DATE_TIME
    ),
    HmppsUnallocatedCase(crn, status)
  )

  @AfterAll
  fun tearDownServer() {
    communityApi.stop()
    hmppsTier.stop()
    oauthMock.stop()
    repository.deleteAll()
  }

  fun setupOauth() {
    val response = response().withContentType(APPLICATION_JSON)
      .withBody(gson.toJson(mapOf("access_token" to "ABCDE", "token_type" to "bearer")))
    oauthMock.`when`(HttpRequest.request().withPath("/auth/oauth/token").withBody("grant_type=client_credentials")).respond(response)
  }

  protected fun singleActiveConvictionResponse(crn: String) {
    val convictionsRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/convictions").withMethod("GET")

    communityApi.`when`(convictionsRequest, Times.exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveConvictionResponse())
    )
  }

  protected fun singleActiveInductionResponse(crn: String) {
    val inductionRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/contact-summary/inductions").withMethod("GET")

    communityApi.`when`(inductionRequest, Times.exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveInductionResponse())
    )
  }

  protected fun offenderSummaryResponse(crn: String) {
    val summaryRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn").withMethod("GET")

    communityApi.`when`(summaryRequest, Times.exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(offenderSummaryResponse())
    )
  }

  protected fun noActiveInductionResponse(crn: String) {
    val inductionRequest =
      HttpRequest.request().withPath("/offenders/crn/$crn/contact-summary/inductions").withMethod("GET")

    communityApi.`when`(inductionRequest, Times.exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody("[]")
    )
  }

  protected fun tierCalculationResponse(crn: String) {
    val request = HttpRequest.request().withPath("/crn/$crn/tier")
    hmppsTier.`when`(request).respond(
      response().withContentType(APPLICATION_JSON).withBody("{\"tierScore\":\"B3\"}")
    )
  }

  protected fun allDeliusResponses(crn: String) {
    singleActiveConvictionResponse(crn)
    singleActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
  }
}
