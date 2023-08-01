package nad1r.techie

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.methods.ForwardMessage
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.*
import org.telegram.telegrambots.meta.bots.AbsSender
import java.util.*

interface MessageHandler {
    fun getMessageService(message: Message, sender: AbsSender)
    fun editMessage(message: Message, sender: AbsSender)
    fun deleteMessage(message: Message, sender: AbsSender)
}

interface AdminService {

    fun callAdminService(message: Message, chatId: Long, sendMessage: SendMessage, sender: AbsSender)
    fun addOperator(message: Message, sendMessage: SendMessage, sender: AbsSender): User
    fun deleteOperator(phoneNumber: String)
    fun checkAdminState(user: User?, message: Message, sendMessage: SendMessage, sender: AbsSender)
    fun getUsers(pageable: Pageable): Page<UserDto>
}

interface UserService {
    fun register(message: Message): User
    fun userStart(user: User, message: Message, sendMessage: SendMessage, sender: AbsSender)

}

interface OperatorService {
    fun callOperatorService(message: Message, sendMessage: SendMessage, sender: AbsSender)
    fun online(message: Message, user: User, sendMessage: SendMessage, sender: AbsSender)
    fun writeAnswer(user: User?, sendMessage: SendMessage, sender: AbsSender)
    fun complete(chatId: Long?, sendMessage: SendMessage, sender: AbsSender)
    fun completeAndOffline(chatId: Long, sendMessage: SendMessage, sender: AbsSender)
}


interface ClientService {
    fun callClientService(message: Message, sendMessage: SendMessage, sender: AbsSender)
    fun stateChooseLang(chatId: Long, lang: Language)
    fun rateOperator(session: Session, sendMessage: SendMessage, sender: AbsSender)
    fun chooseSetting(user: User, sendMessage: SendMessage, sender: AbsSender)
}

interface SessionService {
    fun createSession(user: User)
    fun getSessionChat(sessionId: Long, pageable: Pageable):  Page<UserMessageDto>
    fun getSessions(pageable: Pageable) : Page<SessionDto>

    fun saveSessionRate(sessionId: Long, rate: Short, chatId: Long?): Language
    fun getSessionByUserIdIsActiveFalse(user: User, sendMessage: SendMessage, sender: AbsSender)
    fun getSessionByUserIdIsActiveTrue(user: User, sendMessage: SendMessage, sender: AbsSender): Boolean
    fun getSessionByOperatorIdIsActiveSession(user: User, sendMessage: SendMessage, sender: AbsSender)
    fun getSessionToOperator(operatorId: Long, sender: AbsSender)
    fun calculateAverageRate(operatorId: Long): OperatorRateDto
    fun completeSession(chatId: Long, sendMessage: SendMessage, sender: AbsSender)
    fun deleteMessage(message: Message, sender: AbsSender)
}

interface UserMessageService {
    fun createMessage(message: Message)
    fun sendMessage(operatorId: Long, session: Session)

}

interface GroupAndBotCommunicationService {
    fun messageBuilder(message: Message): MessageDto
    fun sendMessageToChatId(chatId: Long, message: UserMessage)
    fun editMessage(message: Message)
    fun sendNotification(operator: User, client: User)

}
interface CallbackQueryHandle {
    fun callbackQuery(callbackQuery: CallbackQuery?, sender: AbsSender)
    fun callback(chatId: Long, lang: Language, contactLang: String, sendMessage: SendMessage, sender: AbsSender)
    fun deleteMessage(callbackQuery: CallbackQuery?, sender: AbsSender)
    fun rateResult(rate: String, sessionId: String, chatId: Long?): Language
}

interface UserStateService {
    fun stateQuestion(message: Message, user: User, sender: AbsSender)
    fun stateQuestion2(message: Message, user: User, sender: AbsSender)
    fun stateOperator(choseAction: String, sendMessage: SendMessage, user: User, sender: AbsSender)
    fun stateOperator3(sendMessage: SendMessage, sender: AbsSender, chatId: Long)
    fun answerState(message: Message, writeAnswer: String, user: User, sendMessage: SendMessage, sender: AbsSender)
    fun stateSharePhoneNumber(sendMessage: SendMessage, user: User, sender: AbsSender, message: Message)
    fun stateChooseLang(sendMessage: SendMessage, message: Message, user: User, sender: AbsSender)
    fun savePhoneNumber(message: Message, text: String?, sendMessage: SendMessage, user: User, sender: AbsSender): User
    fun userStateSettingLang(sendMessage: SendMessage, user: User, sender: AbsSender, message: Message)

}

interface BotConstantsService {
    fun online(user: User?, sendMessage: SendMessage, sender: AbsSender)
    fun writeAnswer(user: User?, sendMessage: SendMessage, sender: AbsSender)
}

@Service
class UserServiceImpl(
    @Lazy private val userRepository: UserRepository,
    private val menuService: MenuService,
    private val messageHandler: MessageHandler
) : UserService {
    override fun register(message: Message): User {
        val chatId = message.chatId
        val user: User = userRepository.findByChatId(chatId)?.also {
            (it.deleted).runIfTrue {
                it.deleted = false
                userRepository.save(it)
            }
        } ?: run {
            var fullName = message.from.firstName.let { "$it " }
            fullName += message.from.lastName ?: ""
            val newUser = User(chatId, fullName)
            return userRepository.save(newUser)
        }
        return user
    }

    override fun userStart(user: User, message: Message, sendMessage: SendMessage, sender: AbsSender) {
        menuService.menu(user, sender)
        val reply = ReplyKeyBoardMarkupService()
        sendMessage.apply {
            text = ResponseMessages.START_MESSAGE
            replyMarkup = reply.sendLanguageSelection()
        }
        user.state = UserState.CHOOSE_LANG
        userRepository.save(user)
        messageHandler.deleteMessage(message, sender)
        sender.execute(sendMessage)
    }

}

@Service
class ClientServiceImpl(
    private val userRepository: UserRepository,
    private val userStateService: UserStateService,
    @Lazy private val userService: UserService
) : ClientService {
    override fun callClientService(message: Message, sendMessage: SendMessage, sender: AbsSender) {
        val chatId = message.chatId
        val text = message.text
        sendMessage.enableHtml(true)

        val user = userRepository.findByChatId(chatId) ?: throw ChatBotException.UserNotFoundException(chatId)

        when (text) {
            "/change_lang" -> chooseSetting(user, sendMessage, sender)
            else -> {
                when (user.state) {
                    UserState.CHOOSE_LANG, UserState.SETTING_LANG -> {
                        if (message.hasText()) {
                            userStateService.stateChooseLang(sendMessage, message, user, sender)
                        }
                    }

                    UserState.RATE -> {
                        val errorMessage = when (user.language[0]) {
                            Language.UZ -> ResponseMessages.ERROR_RATE_UZ
                            Language.RU -> ResponseMessages.ERROR_RATE_RU
                            Language.EN -> ResponseMessages.ERROR_RATE_EN
                        }
                        sendMessage.text = errorMessage
                        sendMessage.chatId = user.chatId.toString()
                        sender.execute(sendMessage)
                    }

                    UserState.QUESTION -> userStateService.stateQuestion(message, user, sender)
                    UserState.QUESTION2 -> userStateService.stateQuestion2(message, user, sender)
                    UserState.SHARE_PHONE_NUMBER -> userStateService.stateSharePhoneNumber(
                        sendMessage,
                        user,
                        sender,
                        message
                    )

                    else -> {}
                }
            }
        }

        if (user.state == UserState.START && text == "/start") {
            userService.userStart(user, message, sendMessage, sender)
        }
    }

    override fun stateChooseLang(chatId: Long, lang: Language) {
        val user = userRepository.findByChatIdAndDeletedFalse(chatId)
        user.language = mutableListOf(lang)
        user.state = UserState.SHARE_PHONE_NUMBER
        userRepository.save(user)
    }

    override fun rateOperator(session: Session, sendMessage: SendMessage, sender: AbsSender) {
        val reply = ReplyKeyBoardMarkupService()
        sendMessage.chatId = session.client.chatId.toString()
        sendMessage.text = when (session.language) {
            Language.UZ -> ResponseMessages.RATE_OPERATOR_UZ
            Language.RU -> ResponseMessages.RATE_OPERATOR_RU
            Language.EN -> ResponseMessages.RATE_OPERATOR_EN

            else -> ({}).toString()
        }
        sendMessage.replyMarkup = reply.rateOperator(session.id!!)
        sender.execute(sendMessage)
    }
    override fun chooseSetting(user: User, sendMessage: SendMessage, sender: AbsSender) {
        val reply = ReplyKeyBoardMarkupService()
        user.state = UserState.SETTING_LANG
        userRepository.save(user)
        sendMessage.apply {
            text = ResponseMessages.CHOSE_COMMAND
            replyMarkup = reply.sendLanguageSelection()
            chatId = user.chatId.toString()
        }
        sender.execute(sendMessage)

    }
}

@Service
class OperatorServiceImpl(
    @Lazy private val menuService: MenuService,
    @Lazy private val sessionService: SessionService,
    @Lazy private val userStateService: UserStateService,
    private val userRepository: UserRepository,
    @Lazy private val clientService: ClientService,
    @Lazy private val sessionRepository: SessionRepository,
) : OperatorService {
    override fun callOperatorService(message: Message, sendMessage: SendMessage, sender: AbsSender) {
        val chatId = message.chatId
        val text = message.text
        val user = userRepository.findByChatId(chatId)

        if (user != null) {
            menuService.menu(user, sender)

            if (text != "/change_lang") when (user.state) {
                UserState.STATUS_OPERATOR -> userStateService.stateOperator(
                    ResponseMessages.CHOSE_ACTION,
                    sendMessage,
                    user,
                    sender
                )

                UserState.STATUS_OPERATOR3 -> userStateService.stateOperator3(
                    sendMessage,
                    sender,
                    chatId
                )

                UserState.ANSWER_STATE -> userStateService.answerState(
                    message,
                    ResponseMessages.WRITE_ANSWER,
                    user,
                    sendMessage,
                    sender
                )

                else -> {}
            }

            when (text) {
                "/change_lang" -> clientService.chooseSetting(user, sendMessage, sender)
                BotConstant.ONLINE -> online(message, user, sendMessage, sender)
                BotConstant.WRITE_ANSWER -> writeAnswer(user, sendMessage, sender)
                BotConstant.COMPLETE -> complete(chatId, sendMessage, sender)
                BotConstant.COMPLETE_AND_OFFLINE -> completeAndOffline(chatId, sendMessage, sender)
            }
        }
    }

    override fun online(message: Message, user: User, sendMessage: SendMessage, sender: AbsSender) {
        val size = sessionRepository.findSuitableSessionsList(user.language).size
        if (size == 0) {
            sendMessage.text = ResponseMessages.SESSION_IS_EMPTY
        } else {
            sessionService.getSessionToOperator(user.chatId!!, sender!!)
        }
    }

    override fun writeAnswer(user: User?, sendMessage: SendMessage, sender: AbsSender) {
        val reply = ReplyKeyBoardMarkupService()
        user?.state = UserState.ANSWER_STATE
        userRepository.save(user!!)
        sendMessage.apply {
            text = "♻\uFE0F"
            replyMarkup = reply.completeButton()
        }
        sender.execute(sendMessage)
    }

    override fun complete(chatId: Long?, sendMessage: SendMessage, sender: AbsSender) {
        val reply = ReplyKeyBoardMarkupService()
        val user = userRepository.findByChatId(chatId!!)
        val session = sessionRepository.findByOperatorIdAndIsActiveTrue(user?.id!!)
        session?.let {
            session.isActive = false
            sessionRepository.save(it)
        }
        val size = sessionRepository.findSuitableSessionsList(user.language).size
        if (size == 0) {
            sendMessage.text = ResponseMessages.SESSION_IS_EMPTY
            sendMessage.chatId = chatId.toString()
            sendMessage.replyMarkup = reply.onlineOffline()
            sender.execute(sendMessage)
        } else {
            sessionService.getSessionToOperator(chatId, sender)
        }
        val client = session?.client
        client?.let {
            val findById = userRepository.findById(client.id!!)
            val newClient = findById.get()
            newClient.state = UserState.RATE
            userRepository.save(client)
            clientService.rateOperator(session, sendMessage, sender)
        }
        userRepository.save(user)
    }

    @Transactional
    override fun completeAndOffline(chatId: Long, sendMessage: SendMessage, sender: AbsSender) {
        val reply = ReplyKeyBoardMarkupService()
        val user = userRepository.findByChatId(chatId)
        user?.isOnline = false
        user?.state = UserState.STATUS_OPERATOR
        val session = sessionRepository.findByOperatorIdAndIsActiveTrue(user?.id!!)
        session?.let {
            session.isActive = false
            sessionRepository.save(it)
        }
        val client = session?.client
        client?.let {
            val findById = userRepository.findById(client.id!!)
            val newClient = findById.get()
            newClient.state = UserState.RATE
            userRepository.save(client)
            clientService.rateOperator(session, sendMessage, sender)
            sendMessage.text = "♻\uFE0F"
            sendMessage.replyMarkup = reply.onlineOffline()
            sendMessage.chatId = chatId.toString()
            sender.execute(sendMessage)
        }
        userRepository.save(user)
    }
}

@Service
class AdminServiceImpl(
    private val userRepository: UserRepository,
    @Lazy private val menuService: MenuService,
    @Lazy private val sessionRepository: SessionRepository,
    @Lazy private val stateService: UserStateService
) : AdminService {
    @Transactional
    override fun callAdminService(message: Message, chatId: Long, sendMessage: SendMessage, sender: AbsSender) {
        val user = userRepository.findByChatId(chatId)
        menuService.menu(user!!, sender)
        val reply = ReplyKeyBoardMarkupService()
        checkAdminState(user, message, sendMessage, sender)

        when (message.text) {
            BotConstant.SETTING -> handleSettingAction(user, sendMessage, reply, sender)
            BotConstant.ADD_OPERATOR -> handleAddOperator(user, sendMessage, reply, sender)
            BotConstant.REMOVE_OPERATOR -> handleRemoveOperator(user, sendMessage, reply, sender)
            BotConstant.ALL_OPERATORS -> handleAllOperators(user, sendMessage, reply, sender)
            BotConstant.SESSION_HISTORY -> handleSessionHistory(sendMessage, reply, sender)
            BotConstant.MENU -> handleMenu(user, sendMessage, reply, sender)
            else -> {}
        }
    }

    private fun handleSettingAction(
        user: User,
        sendMessage: SendMessage,
        reply: ReplyKeyBoardMarkupService,
        sender: AbsSender
    ) {
        sendMessage.text = ResponseMessages.CRUD_OPERATOR
        sendMessage.replyMarkup = reply.setting(user)
        sender.execute(sendMessage)
    }

    private fun handleAddOperator(
        user: User,
        sendMessage: SendMessage,
        reply: ReplyKeyBoardMarkupService,
        sender: AbsSender
    ) {
        sendMessage.text = ResponseMessages.ENTER_PHONE_NUMBER_UZ
        user.state = UserState.SHARE_PHONE_NUMBER
        userRepository.save(user)
        sendMessage.replyMarkup = reply.getMenu()
        sender.execute(sendMessage)
    }

    private fun handleRemoveOperator(
        user: User,
        sendMessage: SendMessage,
        reply: ReplyKeyBoardMarkupService,
        sender: AbsSender
    ) {
        sendMessage.text = ResponseMessages.DELETE_PHONE_NUMBER
        user.state = UserState.DELETE_PHONE_NUMBER
        userRepository.save(user)
        sendMessage.replyMarkup = reply.getMenu()
        sender.execute(sendMessage)
    }

    private fun handleAllOperators(
        user: User,
        sendMessage: SendMessage,
        reply: ReplyKeyBoardMarkupService,
        sender: AbsSender
    ) {
        sendMessage.text = ResponseMessages.ALL_OPERATORS
        user.state = UserState.GET_ONE_OPERATOR
        userRepository.save(user)
        sendMessage.replyMarkup = reply.getAllOperators()
        sendMessage.replyMarkup = reply.getMenu()
        sender.execute(sendMessage)
    }

    private fun handleSessionHistory(sendMessage: SendMessage, reply: ReplyKeyBoardMarkupService, sender: AbsSender) {
        sendMessage.text = ResponseMessages.VIEW_HISTORY
        sendMessage.replyMarkup = reply.getMenu()
        sender.execute(sendMessage)
    }

    private fun handleMenu(user: User, sendMessage: SendMessage, reply: ReplyKeyBoardMarkupService, sender: AbsSender) {
        sendMessage.text = ResponseMessages.CRUD_OPERATOR
        sendMessage.replyMarkup = reply.setting(user)
        sender.execute(sendMessage)
    }


    override fun addOperator(message: Message, sendMessage: SendMessage, sender: AbsSender): User {
        val phoneNumber = message.text
        userRepository.findByPhoneNumber(phoneNumber)?.let { operator ->
            operator.role = Role.OPERATOR
            operator.state = UserState.STATUS_OPERATOR
            operator.id?.let { sessionRepository.deleteAllByClientId(it) }
            userRepository.save(operator)
            stateService.stateOperator(ResponseMessages.CHOSE_ACTION, sendMessage, operator, sender)
            return operator
        } ?: run {
            val operator = User().apply {
                role = Role.OPERATOR
                state = UserState.STATUS_OPERATOR
                this.phoneNumber = phoneNumber
            }
            return userRepository.save(operator)
        }
    }

    override fun deleteOperator(phoneNumber: String) {
        userRepository.findByPhoneNumber(phoneNumber)?.apply {
            this.id?.let { userRepository.trash(it) }
        }
    }


    override fun checkAdminState(user: User?, message: Message, sendMessage: SendMessage, sender: AbsSender) {
        val reply = ReplyKeyBoardMarkupService()
        when (user?.state) {
            UserState.SHARE_PHONE_NUMBER -> {
                if (!message.text.startsWith("+998")) {
                    sendMessage.text = ResponseMessages.ERROR_PHONE_NUMBER
                } else if (message.text.length != 13) {
                    sendMessage.text = ResponseMessages.SIMPLE_PHONE_NUMBER
                } else {
                    val operator = addOperator(message, sendMessage, sender)

                    if (operator.role == Role.ADMIN) {
                        sendMessage.text = ResponseMessages.SUCCESS_ADD
                        sendMessage.replyMarkup = reply.setting(user)
                    }
                }
            }

            UserState.DELETE_PHONE_NUMBER -> {
                if (!message.text.startsWith("+998")) {
                    sendMessage.text = ResponseMessages.ERROR_PHONE_NUMBER
                } else if (message.text.length != 14) {
                    sendMessage.text = ResponseMessages.SIMPLE_PHONE_NUMBER
                }
                deleteOperator(message.text)
                sendMessage.replyMarkup = reply.setting(user)
            }


            else -> {}
        }
    }

    override fun getUsers(pageable: Pageable): Page<UserDto> {
        val users = userRepository.findAll(pageable)
        return users.map { user ->
            UserDto(
                user.fullName,
                user.phoneNumber,
                user.role,
                user.language
            )
        }
    }
}


@Service
class UserMessageServiceImpl(
    private val userMessageRepository: UserMessageRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    @Lazy private val groupAndBotCommunicationService: GroupAndBotCommunicationService
) : UserMessageService {

    @Transactional
    override fun createMessage(message: Message) {
        val chatId = message.chatId
        val user = userRepository.findByChatId(chatId) ?: throw ChatBotException.UserNotFoundException(chatId)
        val session = user.id?.let {
            sessionRepository.findByClientIdAndIsActiveTrue(it) ?: sessionRepository.findByOperatorIdAndIsActiveTrue(it)
        } ?: run {
            if (user.role == Role.CLIENT)
                sessionRepository.save(Session(user, true, user.language.first()))
            else return
        }

        val messageDto = groupAndBotCommunicationService.messageBuilder(message)
        val userMessage = messageDto.toMessage(
            if (user.role == Role.OPERATOR) MessageType.RESPONSE else MessageType.REQUEST, session
        )
        val savedMessage = userMessageRepository.save(userMessage)
        if (userMessage.type == MessageType.REQUEST && session.operator != null)
            groupAndBotCommunicationService.sendMessageToChatId(session.operator!!.chatId!!, savedMessage)
        else if(userMessage.type == MessageType.RESPONSE)
            groupAndBotCommunicationService.sendMessageToChatId(session.client!!.chatId!!, savedMessage)
    }

    override fun sendMessage(operatorId: Long, session: Session) {
        val operator =
            userRepository.findById(operatorId).orElseThrow { ChatBotException.OperatorNotFoundException(operatorId) }
        val sessionToOperator = session.id?.let {
            userMessageRepository.findAllBySessionIdAndTypeRequestOderOrderByCreatedDate(it)
        }
        sessionToOperator?.map { message ->
            operator.chatId?.let { it1 -> groupAndBotCommunicationService.sendMessageToChatId(it1, message) }
        }
    }

}


interface MessageService {
    fun getMessageService(update: Update, sendMessage: SendMessage)
}


interface CallbackQueryService {
    fun getCallbackQueryService(update: Update, sendMessage: SendMessage)
    fun callbackUz(chatId: Long, lang: Language, sendMessage: SendMessage)
    fun callbackRu(chatId: Long, language: Language, sendMessage: SendMessage)
    fun callbackEn(chatId: Long, language: Language, sendMessage: SendMessage)
}


@Service
class SessionServiceImpl(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val userMessageRepository: UserMessageRepository,
    private val userMessageService: UserMessageService,
    @Lazy private val clientService: ClientService,
    @Lazy private val groupAndBotCommunicationService: GroupAndBotCommunicationService
) : SessionService {

    override fun createSession(user: User) {
        sessionRepository.save(Session(user, true, user.language.first()))
    }

    override fun getSessionChat(sessionId: Long, pageable: Pageable):  Page<UserMessageDto>{
        val sessionChat = userMessageRepository.findAllBySessionIdOrderByCreatedDate(sessionId, pageable)
        return sessionChat.map { message ->
            UserMessageDto(
                text = message.text,
                contentType = message.contentType,
                type = message.type,
                isReply = message.isReply,
                storedId = message.storedId,
                createdDate = message.createdDate
            )
        }
    }


    override fun getSessions(pageable: Pageable): Page<SessionDto> {
        val inactiveSessions = sessionRepository.findAllByIsActiveFalse(pageable)
        return inactiveSessions.map { session ->
            SessionDto(
                id = session.id,
                clientName = session.client.fullName,
                operatorName = session.operator?.fullName,
                rate = session.rate,
                language = session.language,
                createdDate = session.createdDate
            )
        }
    }

    @Transactional
    override fun getSessionToOperator(operatorId: Long, sender: AbsSender) {
        val operator = userRepository.findByChatId(operatorId)
        operator?.id?.let {
            sessionRepository.findByOperatorIdAndIsActiveTrue(it)?.let { session ->
                session.isActive = false
                sessionRepository.save(session)

            }
        }
        operator?.run {
            operator.isOnline = true
            val languageList: List<Language> = operator.language.toList()
            val session = sessionRepository.findSuitableSession(languageList)
            session?.let {
                session.operator = operator
                state = UserState.ANSWER_STATE
                groupAndBotCommunicationService.sendNotification(operator, session.client)
                userRepository.save(operator)
                sessionRepository.save(session)
                operator.id?.let { it1 -> userMessageService.sendMessage(it1, session) }
            }
        }
    }

    override fun calculateAverageRate(operatorId: Long): OperatorRateDto {
        val operator = userRepository.findById(operatorId).orElseThrow {
            ChatBotException.OperatorNotFoundException(operatorId)
        }
        val ratingByOperatorId = sessionRepository.averageRatingByOperatorId(operatorId)
        return OperatorRateDto.toDto(operator, ratingByOperatorId)
    }

    @Transactional
    override fun completeSession(chatId: Long, sendMessage: SendMessage, sender: AbsSender) {
        val reply = ReplyKeyBoardMarkupService()
        val user = userRepository.findByChatId(chatId)
        user?.isOnline = false
        user?.state = UserState.STATUS_OPERATOR
        val session = sessionRepository.findByOperatorIdAndIsActiveTrue(user?.id!!)
        session?.let {
            session.isActive = false
            sessionRepository.save(it)
        }
        val client = session?.client
        client?.let {
            val findById = userRepository.findById(client.id!!)
            val newClient = findById.get()
            newClient.state = UserState.RATE
            userRepository.save(client)
            clientService.rateOperator(session, sendMessage, sender)
            sendMessage.text = "♻\uFE0F"
            sendMessage.replyMarkup = reply.onlineOffline()
            sendMessage.chatId = chatId.toString()
            sender.execute(sendMessage)
        }
        userRepository.save(user)
    }

    override fun saveSessionRate(sessionId: Long, rate: Short, chatId: Long?): Language {
        val session = sessionRepository.findByIdAndDeletedFalse(sessionId)
        session?.let {
            it.rate = rate
            sessionRepository.save(it)
            it.client?.apply {
                state = UserState.QUESTION2
                userRepository.save(this)
            }
            return it.language
        }?: throw ChatBotException.SessionNotFoundException(sessionId)
    }

    override fun getSessionByUserIdIsActiveFalse(user: User, sendMessage: SendMessage, sender: AbsSender) {
        val session = sessionRepository.findByClientIdAndIsActiveTrue(user.id!!)
        if (session != null && session.isActive) {
            if (user.language.contains(Language.UZ))
                sendMessage.text = ResponseMessages.YOUR_SESSION_IS_ACTIVE_UZ
            else if (user.language.contains(Language.RU))
                sendMessage.text = ResponseMessages.YOUR_SESSION_IS_ACTIVE_RU
            else sendMessage.text = ResponseMessages.YOUR_SESSION_IS_ACTIVE_EN
            sendMessage.chatId = user.chatId.toString()
            sender.execute(sendMessage)
        }
    }

    override fun getSessionByUserIdIsActiveTrue(user: User, sendMessage: SendMessage, sender: AbsSender): Boolean {
        val session = sessionRepository.findByClientIdAndIsActiveTrue(user.id!!)
        if (session != null && session.isActive) {
            user.state = UserState.QUESTION2
            userRepository.save(user)
            if (user.language.contains(Language.UZ))
                sendMessage.text = ResponseMessages.YOUR_SESSION_IS_ACTIVE_IN_LANG_UZ
            else if (user.language.contains(Language.RU))
                sendMessage.text = ResponseMessages.YOUR_SESSION_IS_IN_LANG_ACTIVE_RU
            else sendMessage.text = ResponseMessages.YOUR_SESSION_IS_ACTIVE_IN_LANG_EN
            sendMessage.chatId = user.chatId.toString()
            return true
        }
        return false
    }

    override fun getSessionByOperatorIdIsActiveSession(user: User, sendMessage: SendMessage, sender: AbsSender) {
        val session = sessionRepository.findByOperatorIdAndIsActiveTrue(user.id!!)
        if (session != null && session.isActive) {
            if (user.language.contains(Language.UZ))
                sendMessage.text = ResponseMessages.YOUR_SESSION_IS_ACTIVE_UZ
            else if (user.language.contains(Language.RU))
                sendMessage.text = ResponseMessages.YOUR_SESSION_IS_ACTIVE_RU
            else sendMessage.text = ResponseMessages.YOUR_SESSION_IS_ACTIVE_EN
        }
        sendMessage.chatId = user.chatId.toString()
        sender.execute(sendMessage)


    }
    override fun deleteMessage(message: Message, sender: AbsSender) {
        val chatId = message.chatId
        val messageId = message.messageId
        sender.execute(DeleteMessage(chatId.toString(), messageId))
    }

}

@Service
class MessageHandlerImpl(
    @Lazy private val clientService: ClientService,
    private val operatorService: OperatorService,
    @Lazy private val adminService: AdminService,
    @Lazy private val userService: UserService,
    @Lazy private val groupAndBotCommunicationService: GroupAndBotCommunicationService
) : MessageHandler {
    override fun getMessageService(message: Message, sender: AbsSender) {
        val text = message.text
        val chatId = message.chatId

        val sendMessage = SendMessage()
        sendMessage.enableHtml(true)
        sendMessage.chatId = chatId.toString()

        val currentUser = userService.register(message)

        when (currentUser.role) {
            Role.CLIENT -> clientService.callClientService(message, sendMessage, sender)
            Role.OPERATOR -> operatorService.callOperatorService(message, sendMessage, sender)
            Role.ADMIN -> if (text != null) adminService.callAdminService(message, chatId, sendMessage, sender)
        }
    }

    override fun deleteMessage(message: Message, sender: AbsSender) {
        val chatId = message.chatId
        val messageId = message.messageId
        sender.execute(DeleteMessage(chatId.toString(), messageId))
    }

    override fun editMessage(message: Message, sender: AbsSender) {
        groupAndBotCommunicationService.editMessage(message)
    }

}



@Service
class GroupAndBotCommunicationServiceImpl(
    private val telegramBot: TelegramBot,
    private val userMessageRepository: UserMessageRepository,
    private val userRepository: UserRepository,
    @Value("\${private.group.id}") private val PRIVATE_GROUP_ID: Long
):  GroupAndBotCommunicationService {

    override fun sendMessageToChatId(chatId: Long, message: UserMessage) {
        when (message.contentType) {
            ContentType.TEXT -> {
                val sendMessage = SendMessage()
                message.isReply.runIfTrue {
                    val findByTelegramId = userMessageRepository.findByTelegramId(message.replyToId.toString())
                    if (findByTelegramId != null)
                        sendMessage.replyToMessageId = findByTelegramId.sentId?.toInt()!!
                    else {
                        val findBySentId = userMessageRepository.findBySentId(message.replyToId.toString())
                        findBySentId?.let {
                            sendMessage.replyToMessageId = findBySentId.telegramId?.toInt()
                        }
                    }
                }
                sendMessage.chatId = chatId.toString()
                sendMessage.text = message.text.toString()
                try {
                    val sentMessage = telegramBot.execute(sendMessage)
                    message.sentId = sentMessage.messageId.toString()
                    userMessageRepository.save(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ContentType.LOCATION -> {
                val sendLocation = ForwardMessage()
                sendLocation.chatId = chatId.toString()
                sendLocation.fromChatId = PRIVATE_GROUP_ID.toString()
                sendLocation.messageId = message.storedId?.toInt() ?: 0
                try {
                    val sentLocation = telegramBot.execute(sendLocation)
                    message.sentId = sentLocation.messageId.toString()
                    userMessageRepository.save(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            ContentType.PHOTO -> {
                val sendPhoto = SendPhoto()
                if (message.isReply) {
                    val findByTelegramId = userMessageRepository.findByTelegramId(message.replyToId.toString())
                    if (findByTelegramId != null)
                        sendPhoto.replyToMessageId = findByTelegramId.sentId?.toInt()!!
                    else {
                        val findBySentId = userMessageRepository.findBySentId(message.replyToId.toString())
                        findBySentId?.let {
                            sendPhoto.replyToMessageId = findBySentId.telegramId?.toInt()
                        }
                    }
                }
                sendPhoto.chatId = chatId.toString()
                sendPhoto.photo = InputFile(message.storedId.toString())
                message.text?.let {
                    sendPhoto.caption = it
                }
                try {
                    val sentPhoto = telegramBot.execute(sendPhoto)
                    message.sentId = sentPhoto.messageId.toString()
                    userMessageRepository.save(message)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ContentType.FORWARD -> {
                val sendForwardMessage = ForwardMessage()
                sendForwardMessage.chatId = chatId.toString()
                sendForwardMessage.fromChatId = PRIVATE_GROUP_ID.toString()
                sendForwardMessage.messageId = message.storedId?.toInt()!!
                try {
                    telegramBot.execute(sendForwardMessage)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }


            ContentType.GIF -> {
                val sendAnimation = SendAnimation()
                if (message.isReply) {
                    val findByTelegramId = userMessageRepository.findByTelegramId(message.replyToId.toString())
                    if (findByTelegramId != null)
                        sendAnimation.replyToMessageId = findByTelegramId.sentId?.toInt()!!
                    else {
                        val findBySentId = userMessageRepository.findBySentId(message.replyToId.toString())
                        findBySentId?.let {
                            sendAnimation.replyToMessageId = findBySentId.telegramId?.toInt()
                        }
                    }
                }
                sendAnimation.chatId = chatId.toString()
                sendAnimation.animation = InputFile(message.storedId.toString())
                message.text?.let {
                    sendAnimation.caption = it
                }
                try {
                    val sentAnimation = telegramBot.execute(sendAnimation)
                    message.sentId = sentAnimation.messageId.toString()
                    userMessageRepository.save(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            ContentType.DOCUMENT -> {
                val sendDocument = SendDocument()
                if (message.isReply) {
                    val findByTelegramId = userMessageRepository.findByTelegramId(message.replyToId.toString())
                    if (findByTelegramId != null)
                        sendDocument.replyToMessageId = findByTelegramId.sentId?.toInt()!!
                    else {
                        val findBySentId = userMessageRepository.findBySentId(message.replyToId.toString())
                        findBySentId?.let {
                            sendDocument.replyToMessageId = findBySentId.telegramId?.toInt()
                        }
                    }
                }
                sendDocument.chatId = chatId.toString()
                sendDocument.document = InputFile(message.storedId.toString())
                message.text?.let {
                    sendDocument.caption = it
                }
                try {
                    val sentDocument = telegramBot.execute(sendDocument)
                    message.sentId = sentDocument.messageId.toString()
                    userMessageRepository.save(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ContentType.VOICE -> {
                val sendVoice = SendVoice()
                if (message.isReply) {
                    val findByTelegramId = userMessageRepository.findByTelegramId(message.replyToId.toString())
                    if (findByTelegramId != null)
                        sendVoice.replyToMessageId = findByTelegramId.sentId?.toInt()!!
                    else {
                        val findBySentId = userMessageRepository.findBySentId(message.replyToId.toString())
                        findBySentId?.let {
                            sendVoice.replyToMessageId = findBySentId.telegramId?.toInt()
                        }
                    }
                }
                sendVoice.chatId = chatId.toString()
                sendVoice.voice = InputFile(message.storedId.toString())
                message.text?.let {
                    sendVoice.caption = it
                }
                try {
                    val sentVoice = telegramBot.execute(sendVoice)
                    message.sentId = sentVoice.messageId.toString()
                    userMessageRepository.save(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ContentType.VIDEO -> {
                val sendVideo = SendVideo()
                if (message.isReply) {
                    val findByTelegramId = userMessageRepository.findByTelegramId(message.replyToId.toString())
                    if (findByTelegramId != null)
                        sendVideo.replyToMessageId = findByTelegramId.sentId?.toInt()!!
                    else {
                        val findBySentId = userMessageRepository.findBySentId(message.replyToId.toString())
                        findBySentId?.let {
                            sendVideo.replyToMessageId = findBySentId.telegramId?.toInt()
                        }
                    }
                }
                sendVideo.chatId = chatId.toString()
                sendVideo.video = InputFile(message.storedId.toString())
                message.text?.let {
                    sendVideo.caption = it
                }
                try {
                    val sentVideo = telegramBot.execute(sendVideo)
                    message.sentId = sentVideo.messageId.toString()
                    userMessageRepository.save(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ContentType.AUDIO -> {
                val sendAudio = SendAudio()
                if (message.isReply) {
                    val findByTelegramId = userMessageRepository.findByTelegramId(message.replyToId.toString())
                    if (findByTelegramId != null)
                        sendAudio.replyToMessageId = findByTelegramId.sentId?.toInt()!!
                    else {
                        val findBySentId = userMessageRepository.findBySentId(message.replyToId.toString())
                        findBySentId?.let {
                            sendAudio.replyToMessageId = findBySentId.telegramId?.toInt()
                        }
                    }
                }
                sendAudio.chatId = chatId.toString()
                sendAudio.audio = InputFile(message.storedId.toString())
                message.text?.let {
                    sendAudio.caption = it
                }
                try {
                    val sentAudio = telegramBot.execute(sendAudio)
                    message.sentId = sentAudio.messageId.toString()
                    userMessageRepository.save(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            ContentType.STICKER -> {
                val sendSticker = SendSticker()
                if (message.isReply) {
                    val findByTelegramId = userMessageRepository.findByTelegramId(message.replyToId.toString())
                    if (findByTelegramId != null)
                        sendSticker.replyToMessageId = findByTelegramId.sentId?.toInt()!!
                    else {
                        val findBySentId = userMessageRepository.findBySentId(message.replyToId.toString())
                        findBySentId?.let {
                            sendSticker.replyToMessageId = findBySentId.telegramId?.toInt()
                        }
                    }
                }
                sendSticker.chatId = chatId.toString()

                sendSticker.sticker = InputFile(message.storedId)
                try {
                    val sentSticker = telegramBot.execute(sendSticker)
                    message.sentId = sentSticker.messageId.toString()
                    userMessageRepository.save(message)
                } catch (e: Exception) {
                    e.printStackTrace()

                }
            }

            ContentType.VIDEONOTE -> {
                val sendVideoNote = SendVideoNote()
                if (message.isReply) {
                    val findByTelegramId = userMessageRepository.findByTelegramId(message.replyToId.toString())
                    if (findByTelegramId != null)
                        sendVideoNote.replyToMessageId = findByTelegramId.sentId?.toInt()!!
                    else {
                        val findBySentId = userMessageRepository.findBySentId(message.replyToId.toString())
                        findBySentId?.let {
                            sendVideoNote.replyToMessageId = findBySentId.telegramId?.toInt()
                        }
                    }
                }
                sendVideoNote.chatId = chatId.toString()
                sendVideoNote.videoNote = InputFile(message.storedId)
                try {
                    val sentVideoNote = telegramBot.execute(sendVideoNote)
                    message.sentId = sentVideoNote.messageId.toString()
                    userMessageRepository.save(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            else -> {}
        }
    }

    override fun messageBuilder(message: Message): MessageDto {
        val messageDto: MessageDto = MessageDto()
        messageDto.telegramId = message.messageId.toString()
        if (message.forwardFromChat != null) {
            messageDto.contentType = ContentType.FORWARD
            messageDto.storedId =
                sendForwardMessageToPrivateGroup(
                    message.chatId.toString(),
                    message.messageId
                )
        } else if (message.hasLocation()) {
            messageDto.contentType = ContentType.LOCATION
            messageDto.storedId = sendLocationToPrivateGroup(message.location.latitude, message.location.longitude)
        } else if (message.hasPhoto()) {
            messageDto.contentType = ContentType.PHOTO
            if (message.caption != null)
                messageDto.text = message.caption
            val photo: PhotoSize = message.photo.maxBy { photoSize ->
                photoSize.width * photoSize.height
            }
            messageDto.storedId = sendPhotoToPrivateGroup(photo.fileId)
        }
        else if (message.hasText()) {
            messageDto.contentType = ContentType.TEXT
            messageDto.text = message.text
        }
        else if (message.hasSticker()) {
            messageDto.contentType = ContentType.STICKER
            if (message.caption != null)
                messageDto.text = message.caption
            messageDto.storedId = sendStickerToPrivateGroup(message.sticker.fileId)
        }
        else if (message.hasDocument()) {
            messageDto.contentType = ContentType.DOCUMENT
            if (message.caption != null)
                messageDto.text = message.caption
            messageDto.storedId = sendDocumentToPrivateGroup(message.document.fileId)
        }
        else if (message.hasVideo()) {
            messageDto.contentType = ContentType.VIDEO
            if (message.caption != null)
                messageDto.text = message.caption
            messageDto.storedId = sendVideoToPrivateGroup(message.video.fileId)
        }
        else if (message.hasVideoNote()) {
            messageDto.contentType = ContentType.VIDEONOTE
            messageDto.storedId = sendVideoNoteToPrivateGroup(message.videoNote.fileId)
        } else if (message.hasAnimation()) {
            messageDto.contentType = ContentType.GIF
            if (message.caption != null)
                messageDto.text = message.caption
            messageDto.storedId = sendAnimationToPrivateGroup(message.animation.fileId)
        }
        else if (message.hasAudio()) {
            messageDto.contentType = ContentType.AUDIO
            if (message.caption != null)
                messageDto.text = message.caption
            messageDto.storedId = sendAudioToPrivateGroup(message.audio.fileId)
        }
        else if (message.hasVoice()) {
            messageDto.contentType = ContentType.VOICE
            if (message.caption != null)
                messageDto.text = message.caption
            messageDto.storedId = sendVoiceToPrivateGroup(message.voice.fileId)
        }

        (message.isReply()).runIfTrue {
            messageDto.isReply = true
            messageDto.replyToId = message.replyToMessage.messageId.toString()
        }
        return messageDto
    }

    override fun editMessage(message: Message) {
        val messageId = message.messageId.toString()
        val userMsg = userMessageRepository.findByTelegramId(messageId)

        userMsg?.let{
            println(userMsg.toString())
            userMsg.text = message.text
            userMessageRepository.save(userMsg)
            println(userMsg.toString())
            userMsg.session.operator?.let {
                userRepository.findByChatId(message.chatId)?.run {
                    val editMessage = EditMessageText()

                    if (this.role == Role.CLIENT) {
                        editMessage.chatId = userMsg.session.operator!!.chatId.toString()
                        editMessage.messageId = userMsg.sentId!!.toInt()
                        editMessage.text = message.text
                    } else {
                        editMessage.chatId = userMsg.session.client!!.chatId.toString()
                        editMessage.messageId = userMsg.sentId!!.toInt()
                        editMessage.text = message.text
                    }

                    try {
                        telegramBot.execute(editMessage)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun sendPhotoToPrivateGroup(fileId: String?): String? {
        val sendPhoto = SendPhoto()
        sendPhoto.chatId = PRIVATE_GROUP_ID.toString()
        sendPhoto.photo = InputFile(fileId)
        return try {
            val groupPhoto = telegramBot.execute(sendPhoto)
            val photoId = groupPhoto.photo.maxBy {
                it.width * it.height
            }.fileId
            photoId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun sendNotification(operator: User, client: User) {
        val sendMessageToOperator = SendMessage()
        sendMessageToOperator.enableHtml(true)

        val sendMessageToClient = SendMessage()
        sendMessageToClient.chatId = client.chatId.toString()
        sendMessageToOperator.chatId = operator.chatId.toString()
        when (client.language[0]) {
            Language.UZ -> {
                sendMessageToOperator.text = "${client.fullName} ${ResponseMessages.USER_CONNECTED_UZ}"
                sendMessageToClient.text = ResponseMessages.OPERATOR_CONNECTED_UZ
            }

            Language.RU -> {
                sendMessageToOperator.text = "${client.fullName} ${ResponseMessages.USER_CONNECTED_RU}"
                sendMessageToClient.text = ResponseMessages.OPERATOR_CONNECTED_RU
            }

            Language.EN -> {
                sendMessageToOperator.text = "${client.fullName} ${ResponseMessages.USER_CONNECTED_EN}"
                sendMessageToClient.text = ResponseMessages.OPERATOR_CONNECTED_EN
            }
        }
        try {
            telegramBot.execute(sendMessageToOperator)
            telegramBot.execute(sendMessageToClient)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendVoiceToPrivateGroup(fileId: String?): String? {
        val sendVoice = SendVoice()
        sendVoice.chatId = PRIVATE_GROUP_ID.toString()
        sendVoice.voice = InputFile(fileId)
        return try {
            val groupVoice = telegramBot.execute(sendVoice)
            val voiceId = groupVoice.voice.fileId
            voiceId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sendLocationToPrivateGroup(latitude: Double, longitude: Double): String? {
        val sendLocation = SendLocation()
        sendLocation.chatId = PRIVATE_GROUP_ID.toString()
        sendLocation.latitude = latitude
        sendLocation.longitude = longitude
        return try {
            val sentLocation = telegramBot.execute(sendLocation)
            val location = sentLocation.messageId.toString()
            location
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    }

    private fun sendVideoNoteToPrivateGroup(fieldId: String): String? {
        val sendVideoNote = SendVideoNote()
        sendVideoNote.chatId = PRIVATE_GROUP_ID.toString()
        sendVideoNote.videoNote = InputFile(fieldId)
        return try {
            val groupVideoNote = telegramBot.execute(sendVideoNote)
            val fileId = groupVideoNote.videoNote.fileId
            fileId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun sendAnimationToPrivateGroup(fieldId: String): String? {
        val sendAnimation = SendAnimation()
        sendAnimation.chatId = PRIVATE_GROUP_ID.toString()
        sendAnimation.animation = InputFile(fieldId)
        return try {
            val groupAnimation = telegramBot.execute(sendAnimation)
            val animationId = groupAnimation.animation.fileId
            animationId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun sendAudioToPrivateGroup(fieldId: String): String?{
        val sendAudio = SendAudio()
        sendAudio.chatId = PRIVATE_GROUP_ID.toString()
        sendAudio.audio = InputFile(fieldId.toString())
        return try {
            val groupAudio = telegramBot.execute(sendAudio)
            val audioId = groupAudio.audio.fileId
            audioId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    private fun sendStickerToPrivateGroup(fieldId: String): String? {
        val sendSticker = SendSticker()
        sendSticker.chatId = PRIVATE_GROUP_ID.toString()
        sendSticker.sticker = InputFile(fieldId)
        return try {
            val groupSticker = telegramBot.execute(sendSticker)
            val stickerId = groupSticker.sticker.fileId
            stickerId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sendVideoToPrivateGroup(fieldId: String): String? {
        val sendVideo = SendVideo()
        sendVideo.chatId = PRIVATE_GROUP_ID.toString()
        sendVideo.video = InputFile(fieldId)
        return try {
            val groupVideo = telegramBot.execute(sendVideo)
            val videoId = groupVideo.video.fileId
            videoId
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

    private fun sendDocumentToPrivateGroup(fieldId: String): String? {
        val sendDocument = SendDocument()
        sendDocument.chatId = PRIVATE_GROUP_ID.toString()
        sendDocument.document = InputFile(fieldId)
        return try {
            val groupDocument = telegramBot.execute(sendDocument)
            val documentId = groupDocument.document.fileId
            documentId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sendForwardMessageToPrivateGroup(fieldId: String, messageId: Int): String? {
        val forwardMessage = ForwardMessage()
        forwardMessage.chatId = PRIVATE_GROUP_ID.toString()
        forwardMessage.fromChatId = fieldId
        forwardMessage.messageId = messageId
        return try {
            val groupForwardMessage = telegramBot.execute(forwardMessage)
            val fileId = groupForwardMessage.messageId.toString()
            fileId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}

@Service
class CallbackQueryServiceImpl(
    @Lazy private val clientService: ClientService,
    @Lazy private val callbackQueryHandle: CallbackQueryHandle,
    @Lazy private val sessionService: SessionService
) : CallbackQueryHandle {
    override fun callbackQuery(callbackQuery: CallbackQuery?, sender: AbsSender) {
        val chatId = callbackQuery?.message?.chatId
        val data = callbackQuery?.data
        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        when (data) {

            "UZ" -> callback(chatId!!, Language.UZ, ResponseMessages.CONTACT_UZ, sendMessage, sender)
            "EN" -> callback(chatId!!, Language.EN, ResponseMessages.CONTACT_RU, sendMessage, sender)
            "RU" -> callback(chatId!!, Language.RU, ResponseMessages.CONTACT_EN, sendMessage, sender)
            else -> {
                val split = data.toString().split(" ")
                val lang = rateResult(split[0], split[1], chatId)

                sendMessage.text = when (lang) {
                    Language.UZ -> ResponseMessages.THANKS_UZ
                    Language.RU -> ResponseMessages.THANKS_RU
                    Language.EN -> ResponseMessages.THANKS_EN
                }
            }
        }
        callbackQueryHandle.deleteMessage(callbackQuery, sender)
        sender.execute(sendMessage)
    }

    override fun callback(
        chatId: Long,
        lang: Language,
        contactLang: String,
        sendMessage: SendMessage,
        sender: AbsSender
    ) {
        val reply = ReplyKeyBoardMarkupService()
        clientService.stateChooseLang(chatId, lang)
        sendMessage.text = contactLang
        sendMessage.replyMarkup = reply.shareContact(lang)
    }

    override fun deleteMessage(callbackQuery: CallbackQuery?, sender: AbsSender) {
        val chatId = callbackQuery!!.message.chatId
        val messageId = callbackQuery.message.messageId

        val deleteMessage = DeleteMessage()
        deleteMessage.chatId = chatId.toString()
        deleteMessage.messageId = messageId

        sender.execute(deleteMessage)
    }

    override fun rateResult(rate: String, sessionId: String, chatId: Long?): Language {
        return sessionService.saveSessionRate(sessionId.toLong(), rate.toShort(), chatId)
    }
}

@Suppress("UNREACHABLE_CODE")
@Service
class UserStateServiceImpl(
    private val userRepository: UserRepository,
    private val userMessageService: UserMessageService,
    @Lazy private val sessionService: SessionService,
    @Lazy val operatorService: OperatorService
) : UserStateService {
    override fun stateQuestion(
        message: Message,
        user: User,
        sender: AbsSender
    ) {

        val text = message.text
        if (text == "/start" && user.state != UserState.CHOOSE_LANG) {
            sessionService.getSessionByUserIdIsActiveFalse(user, SendMessage(), sender)
        } else {
            userContainsLang(user, message)
        }
    }

    override fun stateQuestion2(
        message: Message,
        user: User,
        sender: AbsSender
    ) {
        val text = message.text
        if (text == "/start") {
            sessionService.getSessionByUserIdIsActiveFalse(user, SendMessage(), sender)
        } else {
            userMessageService.createMessage(message)
        }
    }

    override fun stateOperator(
        choseAction: String,
        sendMessage: SendMessage,
        user: User,
        sender: AbsSender
    ) {
        val reply = ReplyKeyBoardMarkupService()
        user.state = UserState.STATUS_OPERATOR2
        userRepository.save(user)
        sendMessage.text = choseAction
        sendMessage.chatId = user.chatId.toString()
        sendMessage.replyMarkup = reply.onlineOffline()
        sender.execute(sendMessage)
    }

    override fun stateOperator3(
        sendMessage: SendMessage,
        sender: AbsSender,
        chatId: Long
    ) {
        val reply = ReplyKeyBoardMarkupService()
        sendMessage.chatId = chatId.toString()
        sendMessage.replyMarkup = reply.onlineOffline()
        sessionService.getSessionToOperator(chatId, sender)
        sender.execute(sendMessage)
    }

    override fun answerState(
        message: Message,
        writeAnswer: String,
        user: User,
        sendMessage: SendMessage,
        sender: AbsSender
    ) {
        val text = message.text
        if (text == "/start") {
            sessionService.getSessionByOperatorIdIsActiveSession(user, sendMessage, sender)
        } else {
            val constantList = BotConstant::class.java.declaredFields.filter { it.type == String::class.java }
                .map { it.get(BotConstant) as String }.toList()

            constantList.contains(text).runIfFalse {
                userMessageService.createMessage(message)
            }
        }
    }

    override fun stateSharePhoneNumber(sendMessage: SendMessage, user: User, sender: AbsSender, message: Message) {
        if (message.forwardFrom != null) {
            return
        }

        if (message.hasContact()) {
            val contact = message.contact
            val firstName = contact.firstName
            val contactPhoneNumber = contact.phoneNumber

            if (firstName != message.from.firstName) {
                val responseMessage = when (user.language.firstOrNull()) {
                    Language.UZ -> ResponseMessages.INVALID_PHONE_NUMBER_UZ
                    Language.RU -> ResponseMessages.INVALID_PHONE_NUMBER_RU
                    Language.EN -> ResponseMessages.INVALID_PHONE_NUMBER_EN
                    else -> ResponseMessages.INVALID_PHONE_NUMBER_EN // Default to English if user language is not specified
                }

                sendMessage.text = responseMessage
                sender.execute(sendMessage)
            } else {
                savePhoneNumber(message, formatPhoneNumber(contactPhoneNumber), sendMessage, user, sender)
            }
        } else if (message.hasText()) {
            val text = message.text
            if (text.startsWith("+") && text.length == 13) {
                savePhoneNumber(message, text, sendMessage, user, sender)
            } else {
                sendMessage.text = ResponseMessages.ERROR_PHONE_NUMBER
                sendMessage.chatId = message.chatId.toString()
                sender.execute(sendMessage)
            }
        }
    }
    private fun formatPhoneNumber(phoneNumber: String): String {
        return if (!phoneNumber.startsWith("+")) {
            "+$phoneNumber"
        } else {
            phoneNumber
        }
    }

    override fun savePhoneNumber(
        message: Message,
        text: String?,
        sendMessage: SendMessage,
        user: User,
        sender: AbsSender
    ): User {
        userRepository.findByPhoneNumberAndChatIdNull(text!!)?.let { operator ->
            operator.chatId = user.chatId
            operator.language = user.language
            operator.fullName = user.fullName
            userRepository.delete(user)
            userRepository.save(operator)
            operatorService.callOperatorService(message, sendMessage, sender)
            return operator
        }

        val reply = ReplyKeyBoardMarkupService()
        user.apply {
            this.state = UserState.QUESTION
            phoneNumber = text
            userRepository.save(this)
        }
        val responseMessage = when {
            user.language.contains(Language.UZ) -> ResponseMessages.QUESTIONS_UZ
            user.language.contains(Language.RU) -> ResponseMessages.QUESTIONS_RU
            else -> ResponseMessages.QUESTIONS_EN
        }
        sendMessage.text = responseMessage

        if (user.role == Role.CLIENT) {
            reply.deleteReplyMarkup(user.chatId!!, sender)
        }
        sender.execute(sendMessage)
        return user
    }

    override fun userStateSettingLang(sendMessage: SendMessage, user: User, sender: AbsSender, message: Message) {

    }

    override fun stateChooseLang(sendMessage: SendMessage, message: Message, user: User, sender: AbsSender) {
        if (!message.hasText())
            return
        val text = message.text
        when {
            user.state == UserState.SETTING_LANG && text == "/start" -> {
                handleSettingLangStart(user, SendMessage(), sender)
            }

            else -> {
                sendErrorMessage(ResponseMessages.ENTER_ERROR_MESSAGE, message.chatId.toString(), sendMessage, sender)
            }
        }
    }

    private fun handleSettingLangStart(user: User, sendMessage: SendMessage, sender: AbsSender) {
        sessionService.getSessionByUserIdIsActiveFalse(user, sendMessage, sender)
    }

    private fun sendErrorMessage(errorMessage: String, chatId: String, sendMessage: SendMessage, sender: AbsSender) {
        sendMessage.text = errorMessage
        sendMessage.chatId = chatId
        sender.execute(sendMessage)
    }

    fun userContainsLang(user: User, message: Message) {
        userMessageService.createMessage(message)
        user.state = UserState.QUESTION2
        userRepository.save(user)
    }
}
@Service
class BotConstantsServiceImpl(
    @Lazy private val sessionService: SessionService,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : BotConstantsService {
    override fun online(
        user: User?,
        sendMessage: SendMessage,
        sender: AbsSender
    ) {
        val reply = ReplyKeyBoardMarkupService()
        val size = sessionRepository.findSuitableSessionsList(user!!.language).size
        if (size == 0) {
            sendMessage.text = ResponseMessages.SESSION_IS_EMPTY
        } else {
            sessionService.getSessionToOperator(user.chatId!!, sender)
            sendMessage.text = "\uD83D\uDCAB"
            sendMessage.replyMarkup = reply.completeButton()
        }
        sender.execute(sendMessage)
    }

    override fun writeAnswer(user: User?, sendMessage: SendMessage, sender: AbsSender) {
        val reply = ReplyKeyBoardMarkupService()
        user?.state = UserState.ANSWER_STATE
        userRepository.save(user!!)
        sendMessage.text = "♻\uFE0F"
        sendMessage.replyMarkup = reply.completeButton()
        sender.execute(sendMessage)
    }

}