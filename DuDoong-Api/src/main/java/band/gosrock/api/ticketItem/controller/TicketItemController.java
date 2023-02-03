package band.gosrock.api.ticketItem.controller;


import band.gosrock.api.ticketItem.dto.request.ApplyTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest;
import band.gosrock.api.ticketItem.dto.response.GetAppliedOptionGroupsResponse;
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse;
import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionsResponse;
import band.gosrock.api.ticketItem.dto.response.TicketItemResponse;
import band.gosrock.api.ticketItem.service.*;
import band.gosrock.common.annotation.DisableSwaggerSecurity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@Tag(name = "티켓 상품 관련 컨트롤러")
@RestController
@RequestMapping("/v1/events/{eventId}/ticketItems")
@RequiredArgsConstructor
public class TicketItemController {

    public final CreateTicketItemUseCase createTicketItemUseCase;
    public final ApplyTicketOptionUseCase applyTicketOptionUseCase;
    public final GetTicketOptionsUseCase getTicketOptionsUseCase;
    public final GetEventTicketItemsUseCase getEventTicketItemsUseCase;
    public final DeleteTicketItemUseCase deleteTicketItemUseCase;
    public final GetAppliedOptionGroupsUseCase getAppliedOptionGroupsUseCase;

    @Operation(
            summary = "특정 이벤트에 속하는 티켓 상품을 생성합니다.",
            description = "제휴 되지 않은 회원은 티켓 가격 0으로 강제해 보내주세요!")
    @PostMapping
    public TicketItemResponse createTicketItem(
            @RequestBody @Valid CreateTicketItemRequest createTicketItemRequest,
            @PathVariable Long eventId) {
        return createTicketItemUseCase.execute(createTicketItemRequest, eventId);
    }

    @Operation(summary = "옵션을 티켓상품에 적용합니다.")
    @PatchMapping("/{ticketItemId}/option")
    public GetTicketItemOptionsResponse applyTicketOption(
            @RequestBody @Valid ApplyTicketOptionRequest applyTicketOptionRequest,
            @PathVariable Long eventId,
            @PathVariable Long ticketItemId) {
        return applyTicketOptionUseCase.execute(applyTicketOptionRequest, eventId, ticketItemId);
    }

    @Operation(summary = "해당 이벤트의 티켓상품을 모두 조회합니다.")
    @DisableSwaggerSecurity
    @GetMapping
    public GetEventTicketItemsResponse getEventTicketItems(@PathVariable Long eventId) {
        return getEventTicketItemsUseCase.execute(eventId);
    }

    @Operation(summary = "해당 티켓상품의 옵션을 모두 조회합니다.")
    @DisableSwaggerSecurity
    @GetMapping("/{ticketItemId}/options")
    public GetTicketItemOptionsResponse getTicketItemOptions(
            @PathVariable Long eventId, @PathVariable Long ticketItemId) {
        return getTicketOptionsUseCase.execute(eventId, ticketItemId);
    }

    @Operation(summary = "해당 이벤트의 티켓상품 옵션 적용 현황을 모두 조회합니다.")
    @GetMapping("/appliedOptionGroups")
    public GetAppliedOptionGroupsResponse getAppliedOptionGroups(@PathVariable Long eventId) {
        return getAppliedOptionGroupsUseCase.execute(eventId);
    }

    @Operation(summary = "해당 티켓상품을 삭제합니다.")
    @PatchMapping("/{ticketItemId}")
    public GetEventTicketItemsResponse deleteTicketItem(
            @PathVariable Long eventId, @PathVariable Long ticketItemId) {
        return deleteTicketItemUseCase.execute(eventId, ticketItemId);
    }
}
