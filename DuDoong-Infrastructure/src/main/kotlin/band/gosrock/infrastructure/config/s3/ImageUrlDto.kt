package band.gosrock.infrastructure.config.s3

data class ImageUrlDto(
    val url: String,
    val key: String,
    val baseUrl: String? = null,
) {
    companion object {
        @JvmStatic
        fun of(url: String, key: String): ImageUrlDto = ImageUrlDto(url = url, key = key)
    }
}
