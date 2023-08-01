package nad1r.techie

import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.util.Date

data class SessionDto(
    val id: Long? = null,
    val clientName: String? = null,
    val operatorName: String? = null,
    val rate: Short? = null,
    val language: Language? = null,
    val createdDate: Date?  = null
)

data class UserMessageDto(
    val text: String? = null,
    val contentType: ContentType? =  null,
    val type: MessageType? = null,
    val isReply: Boolean = false,
    val storedId: String? = null,
    val createdDate: Date? = null
)
data class SessionHistoryDto(
    val sessionId: Long? = null,
    val clientName: String? = null,
    val operatorName: String? = null,
)

data class UserDto(
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val role: Role? = null,
    val language: MutableList<Language> = mutableListOf()
)

data class OperatorDto(
    val id: Long,
    val fullName: String,
    val phoneNumber: String,
    val rate: Double? = null,
) {
    companion object {
        fun toDto(user: User):OperatorDto {
            return user.run {
                OperatorDto(user.id!!,user.fullName!!,user.phoneNumber!!)
            }
        }
    }

}

data class BaseMessage(val code: Int, val message: String?)


data class MessageDto(
    var text: String? = null,
    var contentType: ContentType? =  null,
    var storedId: String? = null,
    var isReply: Boolean = false,
    var telegramId: String? = null,
    var replyToId: String? = null,
    var sentId: String? = null
){
    fun toMessage(messageType: MessageType, session: Session): UserMessage {
       return UserMessage(
           contentType, messageType, isReply, telegramId, replyToId, sentId, text,  storedId, session
       )
    }
}
data class OperatorRateDto(
    val id: Long,
    val fullName: String,
    val phoneNumber: String,
    val rate: Double? = null
){
    companion object {
        fun toDto(user: User,  rate: Double):OperatorRateDto {
            return user.run {
                OperatorRateDto(user.id!!,user.fullName!!,user.phoneNumber!!, rate)
            }
        }
    }
}