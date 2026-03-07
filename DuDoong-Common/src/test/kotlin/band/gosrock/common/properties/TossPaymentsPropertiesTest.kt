package band.gosrock.common.properties

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
// @ActiveProfiles("common")
class TossPaymentsPropertiesTest {

    @Autowired
    lateinit var tossPaymentsProperties: TossPaymentsProperties

    @Test
    fun `토스페이먼츠 환경변수 값 확인`() {
        val secretKey = tossPaymentsProperties.secretKey
        val mid = tossPaymentsProperties.mid
        assertNotNull(secretKey)
        assertNotNull(mid)
    }
}
