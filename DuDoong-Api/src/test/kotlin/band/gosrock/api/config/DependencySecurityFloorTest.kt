package band.gosrock.api.config

import org.apache.catalina.util.ServerInfo
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 보안 패치가 들어간 버전 밑으로 내려가지 않게 고정한다 (#765).
 * Boot 를 올리거나 루트 build.gradle.kts 의 tomcat.version 고정을 지울 때 실제로 해석된 버전으로 확인한다.
 */
@DisplayName("#765 의존성 보안 하한")
class DependencySecurityFloorTest {

    @Test
    fun `내장 Tomcat 은 multipart CVE-2025-48988·CVE-2025-52520 수정 버전(10_1_43) 이상`() {
        val version = ServerInfo.getServerNumber()
        assertTrue(atLeast(version, "10.1.43"), "Tomcat $version")
    }

    @Test
    fun `mysql-connector-j 는 CVE-2023-22102 수정 버전(8_2_0) 이상`() {
        val version = com.mysql.cj.Constants.CJ_VERSION
        assertTrue(atLeast(version, "8.2.0"), "mysql-connector-j $version")
    }

    private fun atLeast(version: String, floor: String): Boolean {
        val actual = version.split('.').take(3).map { it.takeWhile(Char::isDigit).toInt() }
        val min = floor.split('.').map { it.toInt() }
        return compareValuesBy(actual, min, { it[0] }, { it[1] }, { it[2] }) >= 0
    }
}
