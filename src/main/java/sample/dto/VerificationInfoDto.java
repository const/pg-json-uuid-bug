package sample.dto;

import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldNameConstants
public class VerificationInfoDto {
    private Instant timestamp;
    private VerificationStatus status;
    private String user;
    private String comment;
}
