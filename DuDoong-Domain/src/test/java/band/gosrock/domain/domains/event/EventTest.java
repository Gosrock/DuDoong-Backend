package band.gosrock.domain.domains.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventBasic;
import band.gosrock.domain.domains.event.domain.EventStatus;
import band.gosrock.domain.domains.event.exception.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class EventTest {

    @Mock EventBasic eventBasic;

    Event event;

    final LocalDateTime startAt = LocalDateTime.of(2022, 2, 15, 15, 0);
    final Long runTime = 90L;

    @BeforeEach
    void setup() {
        event = Event.builder().build();
    }

    @Test
    void startAt_가져오기_테스트() {
        // given
        LocalDateTime expectedStartAt = startAt;
        // when
        when(eventBasic.getStartAt()).thenReturn(expectedStartAt);
        event.setEventBasic(eventBasic);
        LocalDateTime actualStartAt = event.getStartAt();
        // then
        assertEquals(expectedStartAt, actualStartAt);
    }

    @Test
    void eventBasic_null_이면_endAt도_반드시_null() {
        // Given
        event.setEventBasic(null);
        // When
        final LocalDateTime endAt = event.getEndAt();
        // Then
        assertNull(endAt);
    }

    @Test
    void endAt_가져오기_테스트() {
        // given
        final EventBasic eventBasic =
                EventBasic.builder().startAt(startAt).runTime(runTime).build();
        event.setEventBasic(eventBasic);
        // when
        final LocalDateTime expectedEndAt = startAt.plusMinutes(runTime);
        final LocalDateTime actualEndAt = event.getEndAt();
        // then
        assertEquals(expectedEndAt, actualEndAt);
    }

    @Test
    public void eventBasic_중복수정은_불가능하다() {
        // given
        // reflection 으로 updated 에 true 강제 주입
        ReflectionTestUtils.setField(event, "isUpdated", true);
        // then
        assertThrows(CannotModifyEventBasicException.class, () -> event.setEventBasic(eventBasic));
    }

    @Test
    public void eventBasic_업데이트_테스트() {
        // given
        EventBasic eventBasic =
                EventBasic.builder().name("test event").startAt(startAt).runTime(runTime).build();
        // when
        event.setEventBasic(eventBasic);
        // then
        assertNotNull(event.getEventBasic());
        assertEquals(eventBasic, event.getEventBasic());
    }

    @Test
    void 이벤트_정산중으로_상태변경_테스트() {
        // given
        final EventStatus originalStatus = event.getStatus();
        final EventStatus expectedStatus = EventStatus.CALCULATING;
        // when
        event.calculate();
        // then
        assertEquals(expectedStatus, event.getStatus());
        assertNotEquals(originalStatus, expectedStatus);
        assertThrows(AlreadyCalculatingStatusException.class, () -> event.calculate());
    }

    @Test
    void 이벤트_종료로_상태변경_테스트() {
        // given
        final EventStatus originalStatus = event.getStatus();
        final EventStatus expectedStatus = EventStatus.CLOSED;
        // when
        event.close();
        // then
        assertEquals(expectedStatus, event.getStatus());
        assertNotEquals(originalStatus, expectedStatus);
        assertThrows(AlreadyCloseStatusException.class, () -> event.close());
    }

    @Test
    void 이벤트_오픈으로_상태변경_테스트() {
        // given
        final EventStatus originalStatus = event.getStatus();
        final EventStatus expectedStatus = EventStatus.OPEN;
        // when
        event.open();
        // then
        assertEquals(expectedStatus, event.getStatus());
        assertNotEquals(originalStatus, expectedStatus);
        assertThrows(AlreadyOpenStatusException.class, () -> event.open());
    }

    @Test
    void 이벤트_준비중으로_상태변경_테스트() {
        // given
        // reflection
        ReflectionTestUtils.setField(event, "status", EventStatus.OPEN);
        final EventStatus originalStatus = event.getStatus();
        final EventStatus expectedStatus = EventStatus.PREPARING;
        // when
        event.prepare();
        // then
        assertEquals(expectedStatus, event.getStatus());
        assertNotEquals(originalStatus, expectedStatus);
        assertThrows(AlreadyPreparingStatusException.class, () -> event.prepare());
    }
}
