package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.SavedEmailsEntity
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.repository.SavedEmailsRepository

@Service
class SavedEmailService(
  private val repository: SavedEmailsRepository,) {

  suspend fun getSavedEmails(userId: String): List<String>{
    if (repository.existsByUserId(userId)) {
      val emails = ArrayList<String>()
      repository.findByUserId(userId).stream().forEach {
          entity ->  emails.add(entity.savedEmail)}
      return emails
    }
    else return emptyList()
  }

  suspend fun saveEmail(userId: String, savedEmail: String) {
    if (!repository.existsByUserIdAndSavedEmail(userId, savedEmail)) {
      repository.save(SavedEmailsEntity(userId =  userId, savedEmail = savedEmail))
    }
  }

  suspend fun deleteSavedEmail(userId: String, savedEmail: String) {
    if (repository.existsByUserIdAndSavedEmail(userId, savedEmail)) {
      repository.delete(repository.findByUserIdAndSavedEmail(userId, savedEmail))
    }
  }
}
