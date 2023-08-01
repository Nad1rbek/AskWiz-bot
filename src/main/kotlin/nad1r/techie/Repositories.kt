package nad1r.techie

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param


@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?

    fun trash(id: Long): T?

    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): Page<T>
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>, entityManager: EntityManager,
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeleted(pageable: Pageable): Page<T> = findAll(isNotDeletedSpecification, pageable)
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }
}

interface UserRepository : BaseRepository<User> {
    fun findByChatId(chatId: Long): User?
    fun findByChatIdAndDeletedFalse(chatId: Long): User
    fun findByPhoneNumber(phoneNumber: String): User?
    fun findByPhoneNumberAndChatIdNull(phoneNumber: String):User?
    fun findAlByRoleAndDeletedFalse(role: Role) :MutableList<User>
}

interface SessionRepository : BaseRepository<Session> {
    fun findByClientIdAndIsActiveTrue(clientId: Long): Session?
    fun findByOperatorIdAndIsActiveTrue(operatorId: Long):  Session?

    fun deleteAllByClientId(clientId: Long)
    @Query("SELECT AVG(s.rate)  FROM sessions s WHERE s.operator_id = ?1 AND s.rate IS NOT NULL",  nativeQuery = true)
    fun averageRatingByOperatorId(operatorId: Long): Double
    @Query("SELECT s FROM sessions s WHERE s.language IN :language AND s.isActive = true AND s.operator IS NULL ")
    fun findSuitableSessionsList( @Param("language") language: List<Language>): List<Session?>
    @Query("SELECT s FROM sessions s WHERE s.language IN :languages AND s.isActive = true AND s.operator IS NULL " +
            " ORDER BY s.createdDate ASC LIMIT 1")
    fun findSuitableSession( @Param("languages") languages: List<Language>): Session?
    fun findAllByIsActiveFalse(pageable: Pageable): Page<Session>
}

interface UserMessageRepository: BaseRepository<UserMessage>{
    fun findByTelegramId(replyToId: String): UserMessage?
    fun findBySentId(id: String): UserMessage?
    @Query("SELECT * FROM user_messages um WHERE um.session_id = ?1 AND um.type = 'REQUEST' " +
            "ORDER BY created_date ASC", nativeQuery = true)
    fun findAllBySessionIdAndTypeRequestOderOrderByCreatedDate(sessionId: Long): List<UserMessage>

    fun findAllBySessionIdOrderByCreatedDate(sessionId: Long, pageable: Pageable): Page<UserMessage>
}