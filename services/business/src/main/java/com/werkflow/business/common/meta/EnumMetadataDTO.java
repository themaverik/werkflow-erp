package com.werkflow.business.common.meta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnumMetadataDTO {
    private String name;
    private String description;
    private List<EnumValueDTO> values;
}
