package band.gosrock.domain.domains.notification.domain

/** 알림 딥링크 대상. [Notification.targetId] 는 HOST 면 hostId, ORDER 면 orderUuid, GIFT 면 giftId (ORDER·GIFT 는 eventId 도 함께 저장) */
enum class NotificationTargetType {
    HOST,
    ORDER,

    /** 선물 (#719) */
    GIFT,
}
