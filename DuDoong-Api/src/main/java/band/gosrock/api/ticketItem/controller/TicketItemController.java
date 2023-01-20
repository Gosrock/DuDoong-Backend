package band.gosrock.api.ticketItem.controller;


import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest;
import band.gosrock.api.ticketItem.dto.response.CreateTicketItemResponse;
import band.gosrock.api.ticketItem.service.CreateTicketItemUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "access-token")
@Tag(name = "티켓 상품 관련 컨트롤러")
@RestController
@RequestMapping("/v1/ticket_items")
@RequiredArgsConstructor
public class TicketItemController {

    public final CreateTicketItemUseCase createTicketItemUseCase;

    @Operation(
            summary = "특정 이벤트에 속하는 티켓 상품을 생성합니다.",
            description = "제휴 되지 않은 회원은 티켓 가격 0으로 강제해 보내주세요!")
    @PostMapping
    public CreateTicketItemResponse createTicketItem(
            @RequestBody @Valid CreateTicketItemRequest createTicketItemRequest) {
        return createTicketItemUseCase.execute(createTicketItemRequest);
    }
}
