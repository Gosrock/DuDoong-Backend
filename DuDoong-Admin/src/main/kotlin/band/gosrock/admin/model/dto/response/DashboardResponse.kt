package band.gosrock.admin.model.dto.response

data class DashboardResponse(
    val totalUsers: Long,
    val todayNewUsers: Long,
    val todayOrders: Long,
    val todayRevenue: Long,
    val activeEvents: Long,
    val todayRefunds: Long,
)
