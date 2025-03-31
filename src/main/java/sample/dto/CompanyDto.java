package sample.dto;

import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class CompanyDto {
    private UUID id;
    private String name;
    private String industry;
    private String description;
    private String url;
    private VerificationInfoDto verified;
    private List<ContactPersonDto> contactPersons;
    private List<OfficeDto> offices;
}
