package com.werkflow.business.common.meta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnumMetadataResponseDTO {
    @JsonProperty("enums")
    private List<EnumMetadataDTO> enums;
}
