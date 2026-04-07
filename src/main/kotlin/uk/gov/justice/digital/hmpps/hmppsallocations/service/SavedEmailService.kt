package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.client.dto.SavedEmailRequest
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.SavedEmailsEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.SavedEmailsRepository

@Service
class SavedEmailService(
  private val repository: SavedEmailsRepository,
) {

  suspend fun getSavedEmails(userId: String): List<String> {
    if (repository.existsByUserId(userId)) {
      val emails = ArrayList<String>()
      repository.findByUserId(userId).stream().forEach { entity ->
        emails.add(entity.savedEmail)
      }
      return emails
    } else {
      return emptyList()
    }
  }

  suspend fun saveEmail(savedEmailRequest: SavedEmailRequest) {
    if (!repository.existsByUserIdAndSavedEmail(savedEmailRequest.userId, savedEmailRequest.email)) {
      repository.save(SavedEmailsEntity(userId = savedEmailRequest.userId, savedEmail = savedEmailRequest.email))
    }
  }

  suspend fun deleteSavedEmail(savedEmailRequest: SavedEmailRequest) {
    if (repository.existsByUserIdAndSavedEmail(savedEmailRequest.userId, savedEmailRequest.email)) {
      repository.delete(repository.findByUserIdAndSavedEmail(savedEmailRequest.userId, savedEmailRequest.email))
    }
  }
}
