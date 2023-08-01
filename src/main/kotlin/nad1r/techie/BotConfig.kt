package nad1r.techie
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@PropertySource("application.yml")
class Config @ConstructorBinding constructor(
    @Value("\${bot.name}") val botName:String,
    @Value("\${bot.token}") val botToken:String
)
