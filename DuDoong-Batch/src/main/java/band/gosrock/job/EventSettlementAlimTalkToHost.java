package band.gosrock.job;


import band.gosrock.domain.common.alarm.SettlementKakaoTalkAlarm;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.config.alilmTalk.NcpHelper;
import band.gosrock.parameter.EventJobParameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class EventSettlementAlimTalkToHost {

    private static final String JOB_NAME = "이벤트정산_알림톡발송_호스트";
    private static final String BEAN_PREFIX = JOB_NAME + "_";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;
    private final UserAdaptor userAdaptor;
    private final NcpHelper ncpHelper;

    @Bean(BEAN_PREFIX + "eventJobParameter")
    @JobScope
    public EventJobParameter eventJobParameter() {
        return new EventJobParameter(eventAdaptor);
    }

    @Qualifier(BEAN_PREFIX + "eventJobParameter")
    private final EventJobParameter eventJobParameter;

    @Bean(JOB_NAME)
    public Job slackUserStatisticJob() {
        return jobBuilderFactory.get(JOB_NAME).preventRestart().start(eventSettlement()).build();
    }

    @Bean(BEAN_PREFIX + "step")
    @JobScope
    public Step eventSettlement() {
        return stepBuilderFactory
                .get(BEAN_PREFIX + "step")
                .tasklet(
                        (contribution, chunkContext) -> {
                            log.info(">>>>> 정산서 전송 안내 알림톡 스탭");
                            Event event = eventJobParameter.getEvent();
                            Host host = hostAdaptor.findById(event.getHostId());
                            User masterUser = userAdaptor.queryUser(host.getMasterUserId());

                            String to =
                                    masterUser
                                            .getProfile()
                                            .getPhoneNumberVo()
                                            .getNaverSmsToNumber();
                            String content =
                                    SettlementKakaoTalkAlarm.creationOf(
                                            masterUser.getProfile().getName());

                            ncpHelper.sendSettlementNcpAlimTalk(
                                    to,
                                    SettlementKakaoTalkAlarm.creationTemplateCode(),
                                    content,
                                    SettlementKakaoTalkAlarm.creationHeaderOf(),
                                    masterUser.getProfile().getEmail(),
                                    event.getEventBasic().getName());
                            return RepeatStatus.FINISHED;
                        })
                .build();
    }
}
