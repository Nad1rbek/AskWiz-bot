package nad1r.techie

interface ResponseMessages {
    companion object {
        const val INVALID_PHONE_NUMBER_UZ = "Siz boshqa foydalanuvchining telefon raqamidan foydalana olmaysiz"
        const val INVALID_PHONE_NUMBER_RU = "Вы не можете использовать номер телефона другого пользователя"
        const val INVALID_PHONE_NUMBER_EN = "You cannot use another user's phone number"
        const val ERROR_RATE_UZ = "Siz xato ball qoydingiz qaytdan uriningSiz xato ball qoydingiz qaytdan urining"
        const val ERROR_RATE_RU = "Вы ввели неверный балл, попробуйте еще раз"
        const val ERROR_RATE_EN = "You have entered an incorrect score, please try again"
        const val CHOSE_COMMAND = "<b>Tilni almashtirishni xoxlasangiz tanlang</b>"
        const val YOUR_SESSION_IS_ACTIVE_UZ = "<b>Kechirasiz siz start qila olmaysiz: ❌\n" +
                "Sizning sessioningi hali aktive: </b>"
        const val YOUR_SESSION_IS_ACTIVE_RU = "<b>Извините, /start нажать нельзя: ❌\n" +
                "Ваша сессия все еще активна: </b>"
        const val YOUR_SESSION_IS_ACTIVE_EN = "<b>Sorry, you can't press star: ❌\n" +
                "Your session is still active: </b>"
        const val YOUR_SESSION_IS_ACTIVE_IN_LANG_UZ = "<b>Kechirasiz siz tilni o'zgartiral olmaysiz: ❌\n" +
                "Sizning sessioningi hali aktive: </b>"
        const val YOUR_SESSION_IS_IN_LANG_ACTIVE_RU = "<b>Извините, вы не можете изменить язык:: ❌\n" +
                "Ваша сессия все еще активна: </b>"
        const val YOUR_SESSION_IS_ACTIVE_IN_LANG_EN = "<b>Sorry, you cannot change the language:: ❌\n" +
                "Your session is still active: </b>"
        const val THANKS_EN = "<b>Thanks for rating: \uD83D\uDE0E\n" +
                "Write if you have any questions: \uD83D\uDCDD\n" +
                "Our operators will contact you: \uD83D\uDC69\u200D\uD83D\uDCBB</b>"
        const val THANKS_UZ = "<b>Baholaganingiz uchun rahmat: \uD83D\uDE0E\n" +
                "Savollaringiz bo'lsa yozing: \uD83D\uDC69\u200D\uD83D\uDCBB\n" +
                "Operatoelarimiz siz bilan bog'lanadi:\uD83D\uDCDD</b>"
        const val THANKS_RU = "<b>Спасибо за оценку! \uD83D\uDE0E\n" +
                "Пишите, если у вас есть вопросы: \uD83D\uDCDD\n" +
                "Наши операторы свяжутся с вами: \uD83D\uDC69\u200D\uD83D\uDCBB</b>"
        const val RATE_OPERATOR_UZ = "Operatorni baholang"
        const val RATE_OPERATOR_RU = "Оцените оператора"
        const val RATE_OPERATOR_EN = "Rate operator"
        const val SESSION_IS_EMPTY = "Savollar xozirda mavjud emas"
        const val SUCCESS_ADD = "Muvofaqiyatli saqlandi"
        const val WRITE_ANSWER = "Javob yozish uchun tugmani bosing"
        const val ALL_OPERATORS = "Botdagi hamma operatorlar ro'yxati"
        const val SIMPLE_PHONE_NUMBER = "Telefon raqam 14 ta belgidan iborat bo'lishi kerak"
        const val DELETE_PHONE_NUMBER = "O'chirmoqchi bo'lgan telefon raqamingizni kiriting"
        const val VIEW_HISTORY = "Hamma session tarixi"
        const val HELP = ""
        const val START_MESSAGE = "<b>Assalomu aleykum botga hush kelibsiz tilni tanlang...\n\n" +
                "Welcome to the bot, choose your language\n\n" +
                "Добро пожаловать в бота, выберите свой язык\n</b>"
        const val ON_CLICK_START = "Iltimos oldin /start tugmasini bosing"
        const val CHOSE_ACTION = "Xolatingizni tanlang"

        const val CONTACT_EN = "Enter contact number"
        const val CONTACT_UZ = "Telefon raqamingizni yuboring"
        const val CONTACT_RU = "Отправьте свой номер телефона"

        const val QUESTIONS_UZ = "<b>Savollaringizni kiriting: \uD83D\uDCDD\n" +
                "Operatorlarimiz siz bilan bog'lanadi: \uD83D\uDC69\u200D\uD83D\uDCBB</b>"
        const val QUESTIONS_RU =
            "<b>Введите ваши вопросы: \uD83D\uDCDD \nнаши операторы свяжутся с вами: \uD83D\uDC69\u200D\uD83D\uDCBB</b>"
        const val QUESTIONS_EN =
            "<b>Enter your questions: \uD83D\uDCDD \nOur operators will contact you: \uD83D\uDC69\u200D\uD83D\uDCBB</b>"

        const val CONNECT_OPERATORS = "Operatorlarimiz siz bilan bo'glanadi"

        const val CRUD_OPERATOR = "Operator"

        const val ENTER_PHONE_NUMBER_UZ = "Telefon raqam kiriting"

        const val ERROR_PHONE_NUMBER = "Telefon raqam xato kiritdingiz ❌"
        const val ENTER_ERROR_MESSAGE = "Xato xabar kiritdingiz ‼\uFE0F"
        const val CHANGED_LANG_UZ = "Til muvofaqiyatli o'zgartirildi"
        const val CHANGED_LANG_RU = "Язык успешно изменен"
        const val CHANGED_LANG_EN = "Language changed successfully"
        const val VIEW_LANG_UZ = "Siz savol qabul qiladigan tillar"
        const val VIEW_LANG_RU = "Языки, на которых вы принимаете вопросы"
        const val VIEW_LANG_EN = "Languages in which you accept questions"

        const val OPERATOR_CONNECTED_EN = "Operator is connected with you 💫"
        const val OPERATOR_CONNECTED_UZ = "Operator siz bilan bog'landi 💫"
        const val OPERATOR_CONNECTED_RU = "Oператор подключен к вам. 💫"
        const val USER_CONNECTED_EN = " is connected with you 💫"
        const val USER_CONNECTED_UZ = " siz bilan bog'landi 💫"
        const val USER_CONNECTED_RU = " подключен к вамб. 💫"
    }
}