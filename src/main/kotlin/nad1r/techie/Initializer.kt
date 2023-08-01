package nad1r.techie

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

@Component
@ComponentScan("nad1r.techie")
class BotInitializer(private val bot: TelegramBot) {

    @EventListener(ContextRefreshedEvent::class)
    fun init() {
        val api = TelegramBotsApi(DefaultBotSession::class.java)
        try {
            api.registerBot(bot)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}