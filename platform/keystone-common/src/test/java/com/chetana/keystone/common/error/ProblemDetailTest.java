package com.chetana.keystone.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void should_serialize_status_title_and_detail() throws Exception {
        var problem = ProblemDetail.of(404, "NOT_FOUND", "Order 42 was not found");

        String json = mapper.writeValueAsString(problem);

        assertThat(json)
                .contains("\"status\":404")
                .contains("\"title\":\"NOT_FOUND\"")
                .contains("\"detail\":\"Order 42 was not found\"");
    }

    @Test
    void should_omit_null_fields() throws Exception {
        var problem = ProblemDetail.of(404, "NOT_FOUND", "gone");

        String json = mapper.writeValueAsString(problem);

        assertThat(json).doesNotContain("\"type\"").doesNotContain("\"instance\"").doesNotContain("\"errors\"");
    }
}
