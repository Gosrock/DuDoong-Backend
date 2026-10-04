package band.gosrock.domain.domains.tag.service

import band.gosrock.domain.domains.tag.domain.Tag
import band.gosrock.domain.domains.tag.domain.TagSeed
import band.gosrock.domain.domains.tag.repository.TagRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

/**
 * 로컬(domain-local, ddl-auto: update) 은 V002 SQL 을 실행하지 않아 tbl_tag 가 비어 있으므로 기동 시 없는 초기 태그만 넣는다.
 * staging / prod 는 V002 [DATA] 로 넣는다.
 */
@Component
@Profile("domain-local")
class LocalTagSeeder(private val tagRepository: TagRepository) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        TagSeed.DEFAULT.forEach { (category, names) ->
            names.forEachIndexed { index, name ->
                if (!tagRepository.existsByCategoryAndName(category, name)) {
                    try {
                        tagRepository.save(Tag(category = category, name = name, sortOrder = index))
                    } catch (e: DataIntegrityViolationException) {
                        // 같은 DB 를 쓰는 다른 로컬 서버가 동시에 넣은 경우 (unique(category, name))
                    }
                }
            }
        }
    }
}
