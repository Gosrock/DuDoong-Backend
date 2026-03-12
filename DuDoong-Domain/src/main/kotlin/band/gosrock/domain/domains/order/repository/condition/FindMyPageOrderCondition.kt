package band.gosrock.domain.domains.order.repository.condition

data class FindMyPageOrderCondition(
    val userId: Long,
    val showing: Boolean,
) {
    companion object {
        @JvmStatic
        fun onShowing(userId: Long): FindMyPageOrderCondition =
            FindMyPageOrderCondition(userId = userId, showing = true)

        @JvmStatic
        fun notShowing(userId: Long): FindMyPageOrderCondition =
            FindMyPageOrderCondition(userId = userId, showing = false)
    }
}
