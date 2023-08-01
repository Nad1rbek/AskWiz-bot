package nad1r.techie
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.bots.AbsSender
import java.util.*

@Component
class ReplyKeyBoardMarkupService(
    private val userRepository: UserRepository? = null
) {

    fun deleteReplyMarkup(chatId: Long, sender: AbsSender): ReplyKeyboard {
        val sendMessage = SendMessage(
            chatId.toString(),
            "\uD83D\uDCAB"
        )
        val replyMarkup = ReplyKeyboardRemove(true)
        sendMessage.replyMarkup = replyMarkup
        sender.execute(sendMessage)
        return replyMarkup
    }

    fun sendLanguageSelection(): InlineKeyboardMarkup {
        val inlineKeyboardMarkup = InlineKeyboardMarkup().apply {
            val inlineKeyboardButton1 = InlineKeyboardButton().apply {
                text = "UZ \uD83C\uDDFA\uD83C\uDDFF"
                callbackData = "UZ"
            }

            val inlineKeyboardButton2 = InlineKeyboardButton().apply {
                text = "EN \uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F"
                callbackData = "EN"
            }

            val inlineKeyboardButton3 = InlineKeyboardButton().apply {
                text = "RU \uD83C\uDDF7\uD83C\uDDFA"
                callbackData = "RU"
            }

            val row = mutableListOf(inlineKeyboardButton1, inlineKeyboardButton2, inlineKeyboardButton3)
            keyboard = listOf(row)
        }

        return inlineKeyboardMarkup
    }

    fun shareContact(lang: Language): ReplyKeyboard {
        val shareContact = ReplyKeyboardMarkup().apply {
            oneTimeKeyboard = true
            resizeKeyboard = true
            keyboard = mutableListOf(KeyboardRow().apply {
                when (lang) {
                    Language.UZ -> add(KeyboardButton("☎️ Kontaktni ulashish").apply {
                        requestContact = true
                    })

                    Language.RU -> add(KeyboardButton("☎️ Поделится контактом").apply {
                        requestContact = true
                    })

                    Language.EN -> add(KeyboardButton("☎️ Share contact").apply {
                        requestContact = true
                    })


                }

            })
        }
        return shareContact
    }

    fun setting(user: User): ReplyKeyboard {
            val adminSetting = ReplyKeyboardMarkup().apply {
                oneTimeKeyboard = true
                resizeKeyboard = true
                keyboard = mutableListOf(KeyboardRow().apply {
                    add(KeyboardButton(BotConstant.ADD_OPERATOR))
                    add(KeyboardButton(BotConstant.REMOVE_OPERATOR))
                },
                    KeyboardRow().apply {
                        add(KeyboardButton(BotConstant.ALL_OPERATORS))
                        add(KeyboardButton(BotConstant.SESSION_HISTORY))
                    })
            }
            return adminSetting

    }

    fun getAllOperators(): ReplyKeyboard? {
        val allOperators = userRepository?.findAlByRoleAndDeletedFalse(Role.OPERATOR)?.map { OperatorDto.toDto(it) }
        val inlineKeyboardMarkup = InlineKeyboardMarkup()
        val keyboardRows = mutableListOf<InlineKeyboardButton>()

        allOperators?.forEachIndexed { index, operator ->
            val inlineKeyboardButton = InlineKeyboardButton().apply {
                text = (index + 1).toString()
                callbackData = "operator_$index" // Use the index of the operator in the list as callbackData
            }
            keyboardRows.add(inlineKeyboardButton)
        }
        inlineKeyboardMarkup.keyboard = listOf(keyboardRows)
        return inlineKeyboardMarkup
    }

    fun getMenu(): ReplyKeyboard? {
        val menu = ReplyKeyboardMarkup().apply {
            oneTimeKeyboard = true
            resizeKeyboard = true
            keyboard = mutableListOf(KeyboardRow().apply {
                add(KeyboardButton(BotConstant.MENU))
            })
        }
        return menu
    }

    fun completeButton(): ReplyKeyboard {
        val complete = ReplyKeyboardMarkup().apply {
            oneTimeKeyboard = true
            resizeKeyboard = true
            keyboard = mutableListOf(KeyboardRow().apply {
                add(KeyboardButton(BotConstant.COMPLETE))
                add(KeyboardButton(BotConstant.COMPLETE_AND_OFFLINE))
            })
        }
        return complete
    }

    fun sendQuestionUZ(): ReplyKeyboard {
        val keyboardMarkup = ReplyKeyboardMarkup()
        keyboardMarkup.resizeKeyboard = true
        keyboardMarkup.oneTimeKeyboard = true

        val row = KeyboardRow()
        row.add("Savol yuborish \uD83D\uDCE8")

        keyboardMarkup.keyboard = listOf(row)

        return keyboardMarkup
    }

    fun sendQuestionRU(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup()
        keyboardMarkup.resizeKeyboard = true
        keyboardMarkup.oneTimeKeyboard = true

        val row = KeyboardRow()
        row.add("Отправить вопрос \uD83D\uDCE8")

        keyboardMarkup.keyboard = listOf(row)

        return keyboardMarkup
    }

    fun sendQuestionEN(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup()
        keyboardMarkup.resizeKeyboard = true
        keyboardMarkup.oneTimeKeyboard = true

        val row = KeyboardRow()
        row.add("Send question \uD83D\uDCE8")

        keyboardMarkup.keyboard = listOf(row)

        return keyboardMarkup
    }

    fun onlineOffline(): ReplyKeyboardMarkup {
        val keyboardMarkup = ReplyKeyboardMarkup()
        keyboardMarkup.resizeKeyboard = true
        keyboardMarkup.oneTimeKeyboard = true

        val row = KeyboardRow()
        row.add("Online ✅")
        keyboardMarkup.keyboard = listOf(row)

        return keyboardMarkup
    }

    fun writeAnswer(): ReplyKeyboard? {
        val writeAnswer = ReplyKeyboardMarkup().apply {
            oneTimeKeyboard = true
            resizeKeyboard = true
            keyboard = mutableListOf(KeyboardRow().apply {
                add(KeyboardButton(BotConstant.WRITE_ANSWER))
            })
        }
        return writeAnswer
    }

    fun rateOperator(sessionId: Long): ReplyKeyboard? {
        val keyboardRows = mutableListOf<List<InlineKeyboardButton>>()

        val row = mutableListOf<InlineKeyboardButton>()
        for (i in 1..5) {
            val rate = InlineKeyboardButton()
            rate.text = i.toString()
            rate.callbackData = "$i $sessionId "
            row.add(rate)
        }

        keyboardRows.add(row)

        val inlineKeyboardMarkup = InlineKeyboardMarkup().apply {
            keyboard = keyboardRows
        }

        return inlineKeyboardMarkup
    }


    fun main() {
        val uzLocale = Locale("uz", "UZ")
        val ruLocale = Locale("ru", "RU")
        val enLocale = Locale("en", "US")

        val uzKeyboard = sendQuestion(uzLocale)
        val ruKeyboard = sendQuestion(ruLocale)
        val enKeyboard = sendQuestion(enLocale)


    }

    fun sendQuestion(locale: Locale): ReplyKeyboard {
        val questionButtonLabel = getResourceString("question_button_label", locale)

        val keyboardMarkup = ReplyKeyboardMarkup()
        keyboardMarkup.resizeKeyboard = true
        keyboardMarkup.oneTimeKeyboard = true

        val row = KeyboardRow()
        row.add(questionButtonLabel)
        keyboardMarkup.keyboard = listOf(row)

        return keyboardMarkup
    }

    fun onlineOffline(locale: Locale): ReplyKeyboard {
        val onlineButtonLabel = getResourceString("online_button_label", locale)

        val keyboardMarkup = ReplyKeyboardMarkup()
        keyboardMarkup.resizeKeyboard = true
        keyboardMarkup.oneTimeKeyboard = true

        val row = KeyboardRow()
        row.add(onlineButtonLabel)
        keyboardMarkup.keyboard = listOf(row)

        return keyboardMarkup
    }

    private fun getResourceString(key: String, locale: Locale): String {
        val resourceBundle = ResourceBundle.getBundle("messages", locale)
        return resourceBundle.getString(key)
    }
}
