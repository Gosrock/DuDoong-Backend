package band.gosrock.api.order.model.dto.request;

import band.gosrock.domain.domains.host.domain.HostRole;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

public enum OrderStage {
    APPROVE_WAITING,CONFIRMED;

}
