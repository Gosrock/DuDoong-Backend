package band.gosrock.domain.domains.comment.repository

import band.gosrock.domain.domains.comment.domain.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<Comment, Long>, CommentCustomRepository {

    @Query(
        nativeQuery = true,
        value = "SELECT * FROM tbl_comment as c WHERE c.event_id = :eventId ORDER BY RAND() DESC LIMIT :offset",
    )
    fun findAllRandom(@Param("eventId") eventId: Long, @Param("offset") limit: Long): List<Comment>

    /** Admin: 키워드(content, nickName)로 댓글 검색 */
    @Query(
        "SELECT c FROM tbl_comment c WHERE " +
            "(:keyword IS NULL OR c.content LIKE %:keyword% OR c.nickName LIKE %:keyword%)"
    )
    fun findAllForAdmin(@Param("keyword") keyword: String?, pageable: Pageable): Page<Comment>
}
