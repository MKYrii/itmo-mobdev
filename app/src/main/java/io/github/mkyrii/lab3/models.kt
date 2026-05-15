package io.github.mkyrii.lab3

data class Message(
    val id: String,
    val from: String,
    val to: String,
    val data: MessageData,
    val time: Long
)

data class MessageData(
    val Text: TextData? = null,
    val Image: ImageData? = null
)

data class TextData(
    val text: String
)

data class ImageData(
    val link: String
)

data class UserCredentials(
    val name: String,
    val pwd: String
)

data class SendMessageRequest(
    val from: String,
    val to: String,
    val data: MessageDataContent,
    val time: Long = System.currentTimeMillis() / 1000
)


data class MessageDataContent(
    val Text: TextContent
)

data class TextContent(
    val text: String
)