package band.gosrock.infrastructure.outer.api.oauth.dto

class UnlinkKaKaoTarget(aud: String) {

    @feign.form.FormProperty("target_id_type")
    val targetIdType: String = "user_id"

    @feign.form.FormProperty("target_id")
    val aud: String = aud

    companion object {
        @JvmStatic
        fun from(aud: String): UnlinkKaKaoTarget = UnlinkKaKaoTarget(aud)
    }
}
