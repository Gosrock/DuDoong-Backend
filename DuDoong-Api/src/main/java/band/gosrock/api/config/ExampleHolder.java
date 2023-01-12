package band.gosrock.api.config;

import io.swagger.v3.oas.models.examples.Example;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExampleHolder {
    private Example holder;
    private String name;
    private int code;
}
