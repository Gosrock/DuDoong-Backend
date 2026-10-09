package band.gosrock.api.v2.operation.controller

import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

/** v2 엑셀 다운로드 응답 (SuccessResponseAdvice 는 ByteArray 를 감싸지 않는다) */
internal object V2Excel {
    /** Swagger 응답 표기에도 쓴다 (#755 — 엑셀 응답이 JSON 으로 보이지 않게) */
    const val XLSX_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private val XLSX = MediaType.parseMediaType(XLSX_MEDIA_TYPE)

    fun attachment(fileName: String, bytes: ByteArray): ResponseEntity<ByteArray> =
        ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
            .contentType(XLSX)
            .body(bytes)
}
