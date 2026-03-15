package band.gosrock.domain.domains.comment.domain

import band.gosrock.domain.common.model.BaseTimeEntity
import band.gosrock.domain.common.vo.CommentInfoVo
import band.gosrock.domain.domains.comment.exception.CommentAlreadyDeleteException
import band.gosrock.domain.domains.comment.exception.CommentNotMatchEventException
import band.gosrock.domain.domains.user.domain.User
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity(name = "tbl_comment")
class Comment() : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    var id: Long? = null
        protected set

    @Column(length = 200)
    var content: String? = null
        protected set

    @Column(length = 15)
    var nickName: String? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    var user: User? = null
        protected set

    var eventId: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    var commentStatus: CommentStatus? = null
        protected set

    constructor(
        content: String?,
        nickName: String?,
        user: User?,
        eventId: Long?,
        commentStatus: CommentStatus?,
    ) : this() {
        this.content = content
        this.nickName = nickName
        this.user = user
        this.eventId = eventId
        this.commentStatus = commentStatus
    }

    fun toCommentInfoVo(): CommentInfoVo = CommentInfoVo.from(this)

    fun delete() {
        if (this.commentStatus == CommentStatus.INACTIVE) {
            throw CommentAlreadyDeleteException.EXCEPTION
        }
        this.commentStatus = CommentStatus.INACTIVE
    }

    fun checkEvent(eventId: Long?) {
        if (eventId != this.eventId) {
            throw CommentNotMatchEventException.EXCEPTION
        }
    }

    companion object {
        @JvmStatic
        fun create(content: String?, nickName: String?, user: User?, eventId: Long?): Comment =
            Comment(content, nickName, user, eventId, CommentStatus.ACTIVE)

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var content: String? = null
        private var nickName: String? = null
        private var user: User? = null
        private var eventId: Long? = null
        private var commentStatus: CommentStatus? = null

        fun content(v: String?) = apply { content = v }
        fun nickName(v: String?) = apply { nickName = v }
        fun user(v: User?) = apply { user = v }
        fun eventId(v: Long?) = apply { eventId = v }
        fun commentStatus(v: CommentStatus?) = apply { commentStatus = v }
        fun build() = Comment(content, nickName, user, eventId, commentStatus)
    }
}
