package sample.dto;

import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class OfficeDto {
    private UUID id;
    private String name;
    private String city;
    private String address;
}
