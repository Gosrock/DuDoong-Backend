package band.gosrock.infrastructure.outer.aws

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest

/**
 * 스테이징 EC2 인스턴스 조회/시작/중지 클라이언트.
 * 인스턴스 롤(IMDS) 자격 증명을 쓰고, 앱 기동 시 AWS 에 접근하지 않도록 Ec2Client 는 첫 사용 시점에 만든다.
 */
@Component
class StagingServerClient(
    @Value("\${aws.staging.instance-id:}") private val instanceId: String,
) {
    private val ec2Client: Ec2Client by lazy {
        Ec2Client.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()
    }

    fun isConfigured(): Boolean = instanceId.isNotBlank()

    fun describe(): StagingServerInfo {
        if (!isConfigured()) return StagingServerInfo(StagingServerState.NOT_CONFIGURED, null)
        val request = DescribeInstancesRequest.builder().instanceIds(instanceId).build()
        val instance = ec2Client.describeInstances(request)
            .reservations()
            .flatMap { it.instances() }
            .firstOrNull()
            ?: return StagingServerInfo(StagingServerState.UNKNOWN, null)
        return StagingServerInfo(
            state = StagingServerState.fromEc2StateName(instance.state()?.nameAsString()),
            launchTime = instance.launchTime(),
        )
    }

    /**
     * 시작 요청 직후의 상태(보통 PENDING)를 돌려준다.
     * 바로 describe 하면 아직 이전 상태가 보일 수 있어 응답의 currentState 를 쓴다.
     */
    fun start(): StagingServerState {
        val response = ec2Client.startInstances(StartInstancesRequest.builder().instanceIds(instanceId).build())
        return StagingServerState.fromEc2StateName(response.startingInstances().firstOrNull()?.currentState()?.nameAsString())
    }

    /** 중지 요청 직후의 상태(보통 STOPPING)를 돌려준다. */
    fun stop(): StagingServerState {
        val response = ec2Client.stopInstances(StopInstancesRequest.builder().instanceIds(instanceId).build())
        return StagingServerState.fromEc2StateName(response.stoppingInstances().firstOrNull()?.currentState()?.nameAsString())
    }
}
