package nad1r.techie

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.*

sealed class ChatBotException(message: String? = null) : RuntimeException(message) {

    abstract fun errorType(): ErrorCode

    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource, vararg array: Any?): BaseMessage {
        return BaseMessage(
            errorType().code,
            errorMessageSource.getMessage(
                errorType().toString(),
                array,
                Locale(LocaleContextHolder.getLocale().language)
            )
        )
    }

    class OperatorNotFoundException(val operatorId: Long) : ChatBotException() {
        override fun errorType(): ErrorCode =
             ErrorCode.OPERATOR_NOT_FOUND

    }
    class UserNotFoundException(val chatId: Long) : ChatBotException() {
        override fun errorType(): ErrorCode =
             ErrorCode.USER_NOT_FOUND

    }

    class SessionNotFoundException(val sessionId: Long) : ChatBotException() {
        override fun errorType(): ErrorCode =
            ErrorCode.SESSION_NOT_FOUND

    }

}
