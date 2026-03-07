package band.gosrock.common.properties

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("common")
class JwtPropertiesTest {

    @Autowired
    lateinit var jwtProperties: JwtProperties

    @Test
    fun `JWT 환경변수값이 불러와지는지 확인`() {
        val accessExp = jwtProperties.accessExp
        val refreshExp = jwtProperties.refreshExp
        assertEquals(accessExp, 3600)
        assertEquals(refreshExp, 3600)
    }
}
