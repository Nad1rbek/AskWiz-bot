package nad1r.techie
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update


@Component
class TelegramBot(
    private val config: Config,
    private val callbackQueryHandle: CallbackQueryHandle? = null,
    private val messageHandler: MessageHandler? = null
) : TelegramLongPollingBot(config.botToken) {


    override fun onUpdateReceived(update: Update) {
        when{
            update.hasEditedMessage() -> messageHandler?.editMessage(update.editedMessage,this)
            update.hasMessage() -> messageHandler?.getMessageService(update.message,this)
            update.hasCallbackQuery() -> callbackQueryHandle?.callbackQuery(update.callbackQuery, this)
        }
    }

    override fun getBotUsername(): String {
        return config.botName
    }

}