package band.gosrock.infrastructure.config.alilmTalk.dto

class MessageDto {

    data class AlimTalkItemButtonBody(
        val plusFriendId: String? = null,
        val templateCode: String? = null,
        val messages: List<AlimTalkItemButtonMessage>? = null,
    )

    data class AlimTalkItemBody(
        val plusFriendId: String? = null,
        val templateCode: String? = null,
        val messages: List<AlimTalkItemMessage>? = null,
    )

    data class AlimTalkButtonBody(
        val plusFriendId: String? = null,
        val templateCode: String? = null,
        val messages: List<AlimTalkButtonMessage>? = null,
    )

    data class AlimTalkItemButtonMessage(
        val to: String? = null,
        val content: String? = null,
        val headerContent: String? = null,
        val item: AlimTalkItem? = null,
        val buttons: List<AlimTalkButton>? = null,
    )

    data class AlimTalkItemMessage(
        val to: String? = null,
        val content: String? = null,
        val headerContent: String? = null,
        val item: AlimTalkItem? = null,
    )

    data class AlimTalkButtonMessage(
        val to: String? = null,
        val content: String? = null,
        val buttons: List<AlimTalkButton>? = null,
    )

    data class AlimTalkButton(
        val type: String? = null,
        val name: String? = null,
        val linkMobile: String? = null,
        val linkPc: String? = null,
    )

    data class AlimTalkItem(
        val list: List<Item>? = null,
    )

    data class Item(
        val title: String? = null,
        val description: String? = null,
    )
}
