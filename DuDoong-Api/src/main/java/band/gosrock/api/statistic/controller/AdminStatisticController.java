package band.gosrock.api.statistic.controller;


import band.gosrock.api.statistic.dto.DashBoardStatisticResponse;
import band.gosrock.api.statistic.useCase.StatisticUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "a2. 어드민 통계관련")
@RestController
@RequestMapping("/v1/events/{eventId}/statistics")
@RequiredArgsConstructor
public class AdminStatisticController {

    private final StatisticUseCase statisticUseCase;

    @Operation(summary = "대시보드 통계를 불러옵니다.")
    @GetMapping
    public DashBoardStatisticResponse getStatistic(@PathVariable Long eventId) {
        return statisticUseCase.execute(eventId);
    }
}
