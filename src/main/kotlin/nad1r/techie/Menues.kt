package nad1r.techie

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

interface MenuService {
    fun menu(user: User, sender: AbsSender)

}

@Component
class MenuImpl : MenuService {
    override fun menu(user: User, sender: AbsSender) {
        try {
            val listCommands = mutableListOf<BotCommand>()

            if (user.role == Role.ADMIN) listCommands.add(BotCommand("/setting", "set your preferences"))
            else {
                listCommands.add(BotCommand("/change_lang", "language switching"))
                listCommands.add(BotCommand("/start", "enter start"))
            }

            sender.execute(SetMyCommands(listCommands, BotCommandScopeDefault(), null))
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}