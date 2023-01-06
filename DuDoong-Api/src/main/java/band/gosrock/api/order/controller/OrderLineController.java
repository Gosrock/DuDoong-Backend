package band.gosrock.api.order.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "access-token")
@Tag(name = "장바구니 관련 컨트롤러")
@RestController
@RequestMapping("/v1/orderLines")
public class OrderLineController {

}
