package com.paraiso.server.util

import com.paraiso.domain.messageTypes.MessageType
import com.paraiso.domain.users.Country
import com.paraiso.domain.users.UserResponse
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.util.reflect.typeInfo
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.nio.charset.Charset
import com.paraiso.domain.messageTypes.TypeMapping as TypeMappingDomain
import com.paraiso.domain.messageTypes.Message as MessageDomain
import com.paraiso.domain.messageTypes.DirectMessage as DirectMessageDomain
import com.paraiso.domain.users.UserResponse as UserResponseDomain

val safeList: Safelist = Safelist()
    .addTags(
        "p", "br", "strong", "em", "u", "s", "code", "pre", "blockquote",
        "ul", "ol", "li", "a", "h1", "h2", "h3", "h4", "h5", "h6"
    )
    .addAttributes("a", "href", "target", "rel")
    .addProtocols("a", "href", "http", "https", "mailto")
    .preserveRelativeLinks(true)

fun determineMessageType(frame: Frame): MessageType? {
    return try {
        val rawText = when (frame) {
            is Frame.Text -> frame.readText()
            else -> return null // Not a text frame
        }

        val jsonElement = Json.parseToJsonElement(rawText)
        val typeMapping = jsonElement.jsonObject["typeMapping"]?.jsonObject

        typeMapping?.keys?.firstOrNull()?.let { key ->
            MessageType.valueOf(key)
        }
    } catch (e: Exception) {
        println("Error determining message type: $e")
        null
    }
}

suspend inline fun <reified T> WebsocketContentConverter.findCorrectConversion(
    frame: Frame, clean: Boolean
): T? =
    try {
        //clean full json object if set to clean
        val cleanFrame = if(clean){
            when (frame) {
                is Frame.Text -> {
                    val rawText = frame.readText()

                    val safeHtml = Jsoup.clean(
                        rawText,
                        safeList
                    )

                    Frame.Text(safeHtml)
                }
                else -> frame
            }
        }else{ frame }
        this.deserialize(
            Charset.defaultCharset(),
            typeInfo<T>(),
            cleanFrame
        ) as? T
    } catch (e: Exception) {
        println(e)
        null
    }

fun cleanValue(value: String?) = value?.let{
    Jsoup.clean(
        it,
        safeList
    )
}
fun MessageDomain.cleanMessage(): MessageDomain =
    this.copy(
        title = cleanValue(this.title),
        content = cleanValue(this.content)
    )

fun DirectMessageDomain.cleanDirectMessage(): DirectMessageDomain =
    this.copy(
        content = cleanValue(this.content)
    )

fun UserResponseDomain.cleanUser(): UserResponseDomain =
    this.copy(
        name = cleanValue(
            this.name
        ),
        fullName = cleanValue(
            this.fullName
        ),
        email = cleanValue(
            this.email
        ),
        image = this.image.copy(
          url = cleanValue(this.image.url)
        ),
        about = cleanValue(
            this.about
        ),
        location = this.location?.copy(
            city = cleanValue(this.location?.city),
            state = cleanValue(this.location?.state),
            country = Country(
                name = cleanValue(this.location?.country?.name),
                code = cleanValue(this.location?.country?.code),
            )
        )
    )


//fun UserResponseDomain.cleanUser(): UserResponseDomain =
//    user: UserResponseDomain
//){
//
//}

suspend inline fun <reified T> WebSocketServerSession.sendTypedMessage(messageType: MessageType, data: T) =
    sendSerialized<TypeMappingDomain<T>>(
        TypeMappingDomain(
            mapOf(messageType to data)
        )
    )
