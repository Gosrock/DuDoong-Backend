package band.gosrock.domain.domains.tag

import band.gosrock.domain.domains.tag.domain.TagSeed
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TagSeedTest {

    /** 로컬 시더 / 테스트 fixture 가 쓰는 [TagSeed] 와 prod 에 실행할 V002 [DATA] 시드가 같아야 한다 */
    @Test
    fun `TagSeed 와 V002 SQL 의 태그 시드가 같다`() {
        val sql = File("../db/migration/V002__705_event_prep.sql").readText()
        val row = Regex("""\(NOW\(\), NOW\(\), '(\w+)', '([^']+)', (\d+)\)""")
        val fromSql = row.findAll(sql).map { Triple(it.groupValues[1], it.groupValues[2], it.groupValues[3].toInt()) }.toList()
        val fromSeed = TagSeed.DEFAULT.flatMap { (category, names) ->
            names.mapIndexed { index, name -> Triple(category.name, name, index) }
        }
        assertEquals(fromSeed, fromSql)
    }
}
