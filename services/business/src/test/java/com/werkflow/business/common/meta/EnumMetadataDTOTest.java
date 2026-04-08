package com.werkflow.business.common.meta;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class EnumMetadataDTOTest {

    @Test
    void testEnumValueDTOStructure() {
        EnumValueDTO value = EnumValueDTO.builder()
            .value("DRAFT")
            .label("Draft")
            .description("Initial state, not yet submitted")
            .build();

        assertEquals("DRAFT", value.getValue());
        assertEquals("Draft", value.getLabel());
        assertEquals("Initial state, not yet submitted", value.getDescription());
    }

    @Test
    void testEnumMetadataDTOStructure() {
        EnumValueDTO value1 = EnumValueDTO.builder()
            .value("DRAFT")
            .label("Draft")
            .description("Initial state")
            .build();

        EnumMetadataDTO metadata = EnumMetadataDTO.builder()
            .name("PrStatus")
            .description("Purchase request lifecycle status")
            .values(Arrays.asList(value1))
            .build();

        assertEquals("PrStatus", metadata.getName());
        assertEquals(1, metadata.getValues().size());
        assertEquals("DRAFT", metadata.getValues().get(0).getValue());
    }

    @Test
    void testEnumMetadataResponseStructure() {
        EnumValueDTO value = EnumValueDTO.builder()
            .value("DRAFT")
            .label("Draft")
            .description("Initial state")
            .build();

        EnumMetadataDTO enum1 = EnumMetadataDTO.builder()
            .name("PrStatus")
            .description("Purchase request status")
            .values(Arrays.asList(value))
            .build();

        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
            .enums(Arrays.asList(enum1))
            .build();

        assertEquals(1, response.getEnums().size());
        assertEquals("PrStatus", response.getEnums().get(0).getName());
    }

    @Test
    void testEnumMetadataResponseSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        EnumValueDTO value = EnumValueDTO.builder()
            .value("DRAFT")
            .label("Draft")
            .description("Initial state")
            .build();

        EnumMetadataDTO enum1 = EnumMetadataDTO.builder()
            .name("PrStatus")
            .description("Purchase request status")
            .values(Arrays.asList(value))
            .build();

        EnumMetadataResponseDTO response = EnumMetadataResponseDTO.builder()
            .enums(Arrays.asList(enum1))
            .build();

        String json = mapper.writeValueAsString(response);
        assertTrue(json.contains("PrStatus"));
        assertTrue(json.contains("DRAFT"));
        assertTrue(json.contains("enums"));
    }
}
