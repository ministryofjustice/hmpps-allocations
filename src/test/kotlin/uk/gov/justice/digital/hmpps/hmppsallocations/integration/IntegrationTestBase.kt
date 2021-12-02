package uk.gov.justice.digital.hmpps.hmppsallocations.integration

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.Times.exactly
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType.APPLICATION_JSON
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.offenderSummaryResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveAndInactiveConvictionsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveConvictionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.singleActiveInductionResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.twoActiveConvictionsResponse
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.UnallocatedCasesRepository
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsEvent
import uk.gov.justice.digital.hmpps.hmppsallocations.listener.HmppsUnallocatedCase
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

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
    tierCalculationSqsClient.purgeQueue(PurgeQueueRequest(tierCalculationQueue.queueUrl))
    setupOauth()
  }

  private val hmppsDomainQueue by lazy { hmppsQueueService.findByQueueId("hmppsdomainqueue") ?: throw MissingQueueException("HmppsQueue hmppsdomainqueue not found") }
  private val tierCalculationQueue by lazy { hmppsQueueService.findByQueueId("tiercalculationqueue") ?: throw MissingQueueException("HmppsQueue tiercalculationqueue not found") }
  private val hmppsDomainTopic by lazy { hmppsQueueService.findByTopicId("hmppsdomaintopic") ?: throw MissingQueueException("HmppsTopic hmppsdomaintopic not found") }
  protected val hmppsDomainSqsClient by lazy { hmppsDomainQueue.sqsClient }
  protected val tierCalculationSqsClient by lazy { tierCalculationQueue.sqsClient }
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

  protected fun unallocatedCaseEvent(crn: String) = HmppsEvent(
    "ALLOCATION_REQUIRED", 0, "some event description", "http://dummy.com",
    ZonedDateTime.now().format(
      ISO_ZONED_DATE_TIME
    ),
    HmppsUnallocatedCase(crn)
  )

  protected fun tierCalculationEvent(
    crn: String
  ) = """
    {
        "Type": "Notification",
        "MessageId": "51d4fd16-5462-5d81-a985-a737297b10e8",
        "TopicArn": "arn:aws:sns:eu-west-2:754256621582:cloud-platform-Digital-Prison-Services-e29fb030a51b3576dd645aa5e460e573",
        "Message": "{\"crn\":\"$crn\",\"calculationId\":\"e6c61816-3c02-4664-b967-8e524f08d551\"}",
        "Timestamp": "2021-12-01T13:38:33.522Z",
        "SignatureVersion": "1",
        "Signature": "fG/RaDvbRcFbUCC+qScsOSqv9AF17Pyz8Z13YKCTfHoM4x77Mi8LyMeSGkh4Aflt77YEcrHihQwavKkQZT5f4tayWNtiESo86kv32wXgv2LiNzQ/n4+c74ToInhaD60owXgA9HJ8s+wvekXVkaUTffZTNwZBd91lORXP3Na07R+uLR6Ic4bS4UwTx+uBSN5Y77gZ3eKG3p1tiA8Iihc67kVa+GOUnT272hmR7p3tnNnIWA40pdBmwAWarHKxRFbRpJLC9U2ttwG/K/aQKG8y2GrZYO0UPgf8+5hQkCZ4xK9IoHDrZafnPa3uGbmqkh7tUNDFHtFKEDH+wQNigewDZQ==",
        "SigningCertURL": "https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-7ff5318490ec183fbaddaa2a969abfda.pem",
        "UnsubscribeURL": "https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:754256621582:cloud-platform-Digital-Prison-Services-e29fb030a51b3576dd645aa5e460e573:9000a6de-4582-4477-aeb6-6d3b5641895e",
        "MessageAttributes": {
            "eventType": {
                "Type": "String",
                "Value": "TIER_CALCULATION_COMPLETE"
            }
        }
    }
  """.trimIndent()

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
    oauthMock.`when`(request().withPath("/auth/oauth/token").withBody("grant_type=client_credentials")).respond(response)
  }

  protected fun singleActiveConvictionResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions").withQueryStringParameter(Parameter("activeOnly", "true"))

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveConvictionResponse())
    )
  }

  protected fun singleActiveConvictionResponseForAllConvictions(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveConvictionResponse())
    )
  }

  protected fun singleActiveInductionResponse(crn: String) {
    val inductionRequest =
      request().withPath("/offenders/crn/$crn/contact-summary/inductions")

    communityApi.`when`(inductionRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveInductionResponse())
    )
  }

  protected fun offenderSummaryResponse(crn: String) {
    val summaryRequest =
      request().withPath("/offenders/crn/$crn")

    communityApi.`when`(summaryRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(offenderSummaryResponse())
    )
  }

  protected fun noActiveInductionResponse(crn: String) {
    val inductionRequest =
      request().withPath("/offenders/crn/$crn/contact-summary/inductions")

    communityApi.`when`(inductionRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody("[]")
    )
  }

  protected fun tierCalculationResponse(crn: String): HttpRequest {
    val request = request().withPath("/crn/$crn/tier")
    hmppsTier.`when`(request).respond(
      response().withContentType(APPLICATION_JSON).withBody("{\"tierScore\":\"B3\"}")
    )
    return request!!
  }

  protected fun twoActiveConvictionsResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")

    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(twoActiveConvictionsResponse())
    )
  }

  protected fun singleActiveAndInactiveConvictionsResponse(crn: String) {
    val convictionsRequest =
      request().withPath("/offenders/crn/$crn/convictions")
    communityApi.`when`(convictionsRequest, exactly(1)).respond(
      response().withContentType(APPLICATION_JSON).withBody(singleActiveAndInactiveConvictionsResponse())
    )
  }

  protected fun allDeliusResponses(crn: String) {
    singleActiveConvictionResponse(crn)
    singleActiveInductionResponse(crn)
    offenderSummaryResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)
  }

  private fun getNumberOfMessagesCurrentlyOnQueue(client: AmazonSQS, queueUrl: String): Int? {
    val queueAttributes = client.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }
  protected fun whenCalculationQueueIsEmpty() {
    await untilCallTo {
      getNumberOfMessagesCurrentlyOnQueue(
        tierCalculationSqsClient,
        tierCalculationQueue.queueUrl
      )
    } matches { it == 0 }
  }

  protected fun whenCalculationMessageHasBeenProcessed() {
    await untilCallTo {
      getNumberOfMessagesCurrentlyNotVisibleOnQueue(
        tierCalculationSqsClient,
        tierCalculationQueue.queueUrl
      )
    } matches { it == 0 }
  }

  private fun getNumberOfMessagesCurrentlyNotVisibleOnQueue(client: AmazonSQS, queueUrl: String): Int? {
    val queueAttributes = client.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessagesNotVisible"))
    return queueAttributes.attributes["ApproximateNumberOfMessagesNotVisible"]?.toInt()
  }
}
