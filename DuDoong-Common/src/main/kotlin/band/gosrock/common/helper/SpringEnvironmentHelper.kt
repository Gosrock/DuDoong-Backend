package band.gosrock.common.helper

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils

@Component
class SpringEnvironmentHelper(
    private val environment: Environment,
) {
    private val PROD = "prod"
    private val STAGING = "staging"
    private val DEV = "dev"
    private val LOCAL = "local"
    private val PROD_AND_STAGING = listOf("staging", "prod")

    fun isProdProfile(): Boolean {
        val currentProfile = environment.activeProfiles.toList()
        return currentProfile.contains(PROD)
    }

    fun isStagingProfile(): Boolean {
        val currentProfile = environment.activeProfiles.toList()
        return currentProfile.contains(STAGING)
    }

    fun isDevProfile(): Boolean {
        val currentProfile = environment.activeProfiles.toList()
        return currentProfile.contains(DEV)
    }

    /** 개발용 API 를 열어도 되는 프로필 (허용 목록). local·dev 이면서 staging·prod 가 아닐 때만 true */
    fun isLocalOrDevProfile(): Boolean {
        val currentProfile = environment.activeProfiles.toList()
        return CollectionUtils.containsAny(currentProfile, listOf(LOCAL, DEV)) &&
            !CollectionUtils.containsAny(currentProfile, PROD_AND_STAGING)
    }

    fun isProdAndStagingProfile(): Boolean {
        val currentProfile = environment.activeProfiles.toList()
        return CollectionUtils.containsAny(PROD_AND_STAGING, currentProfile)
    }
}
