package nad1r.techie

import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.Temporal
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false,
)

@Entity(name = "users")
class User(
    var chatId: Long? = null,
    var fullName: String? = null,
    var phoneNumber: String? = null,
    var isOnline: Boolean? = false,
    @Enumerated(STRING) var role: Role = Role.CLIENT,
    @Enumerated(STRING) var language: MutableList<Language> = mutableListOf(),
    @Enumerated(STRING) var state: UserState = UserState.START,
) : BaseEntity()


@Entity(name = "sessions")
class Session(
    @ManyToOne var client: User,
    var isActive: Boolean = true,
    @Enumerated(STRING) var language: Language,
    @ManyToOne var operator: User? = null,
    var rate: Short? = null,
    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL]) var messages: MutableList<UserMessage> = mutableListOf(),
) : BaseEntity()

@Entity(name = "user_messages")
class UserMessage(
    @Column(length = 10)
    @Enumerated(STRING) var contentType: ContentType? = null,
    @Enumerated(STRING) var type: MessageType,
    var isReply : Boolean = false,
    var telegramId: String? = null,
    var replyToId: String? = null,
    var sentId:  String? = null,
    @Column(columnDefinition = "text")
    var text: String? = null,
    var storedId: String? = null,
    @ManyToOne var session: Session,
) : BaseEntity()

