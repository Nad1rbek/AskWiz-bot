package nad1r.techie

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestControllerAdvice
class ExceptionHandlers(
    private val errorMessageSource: ResourceBundleMessageSource
) {
    @ExceptionHandler(ChatBotException::class)
    fun handleException(exception: ChatBotException): ResponseEntity<BaseMessage> {
        return when (exception) {
            is ChatBotException.OperatorNotFoundException -> ResponseEntity.badRequest()
                .body(exception.getErrorMessage(errorMessageSource, exception.operatorId))
            is ChatBotException.SessionNotFoundException -> ResponseEntity.badRequest()
                .body(exception.getErrorMessage(errorMessageSource, exception.sessionId))
            is ChatBotException.UserNotFoundException -> ResponseEntity.badRequest()
                .body(exception.getErrorMessage(errorMessageSource, exception.chatId))
        }
    }
}

@RestController
@RequestMapping("/api/v1")
class ChatBotController(
    private val sessionService: SessionService,
    private val adminService: AdminService
) {
    @GetMapping("/average-operator-rate/{id}")
    fun start(@PathVariable id: Long) = sessionService.calculateAverageRate(id)

    @GetMapping("/sessions")
    fun getSessions(pageable: Pageable) = sessionService.getSessions(pageable)

    @GetMapping("/session/{id}")
    fun getSessionChat(@PathVariable id: Long,  pageable: Pageable) = sessionService.getSessionChat(id, pageable)

    @GetMapping("/users")
    fun getUsers(pageable: Pageable) = adminService.getUsers(pageable)

}



