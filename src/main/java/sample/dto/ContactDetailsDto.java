package sample.dto;

import lombok.*;
import lombok.experimental.FieldNameConstants;


@Setter
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class ContactDetailsDto {
    private ContactType type;
    private String value;
}
