package nad1r.techie
import java.io.Serializable

enum class Role() {
    ADMIN, CLIENT, OPERATOR
}

enum class Language {
    UZ, RU, EN
}

enum class MessageType() {
    REQUEST, RESPONSE
}

enum class UserState() {
    START,CHOOSE_LANG,
    SHARE_PHONE_NUMBER,
    QUESTION,
    DELETE_PHONE_NUMBER,
    GET_ONE_OPERATOR,
    ALL_OPERATORS,
    STATUS_OPERATOR,
    ANSWER_STATE,
    QUESTION2,
    STATUS_OPERATOR2,
    STATUS_OPERATOR3,
    RATE,
    SETTING_LANG
}

enum class ContentType {
    TEXT, PHOTO, VIDEO, DOCUMENT, STICKER, AUDIO, VOICE, FORWARD, GIF, VIDEONOTE, LOCATION
}

enum class ErrorCode(val code: Int) {
    OPERATOR_NOT_FOUND(100),
    USER_NOT_FOUND(101),
    SESSION_NOT_FOUND(102)
}
