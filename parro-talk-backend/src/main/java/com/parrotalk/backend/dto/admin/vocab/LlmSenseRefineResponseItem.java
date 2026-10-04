package com.parrotalk.backend.dto.admin.vocab;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LlmSenseRefineResponseItem(
        @JsonProperty("index") int index,
        @JsonProperty("cefrLevel") String cefrLevel,
        @JsonProperty("definitionEn") String definitionEn,
        @JsonProperty("definitionVn") String definitionVn,
        @JsonProperty("exampleEn") String exampleEn,
        @JsonProperty("exampleVn") String exampleVn
) {}
