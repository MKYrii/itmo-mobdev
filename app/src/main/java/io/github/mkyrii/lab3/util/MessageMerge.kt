package io.github.mkyrii.lab3.util

import io.github.mkyrii.lab3.Message

object MessageMerge {

    fun sortChronologically(messages: List<Message>): List<Message> =
        messages.distinctBy { it.id }.sortedWith(
            compareBy<Message> { it.time }.thenBy { it.id }
        )

    fun merge(server: List<Message>, pending: List<Message>): List<Message> =
        sortChronologically(server + pending)
}
