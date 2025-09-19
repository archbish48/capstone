package com.community.demo.dto.inquiry;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FastApiBuildResult {
    private String message;

    @JsonProperty("source_file")
    private String sourceFile;

    @JsonProperty("docx_file")
    private String docxFile;

    @JsonProperty("markdown_file")
    private String markdownFile;

    @JsonProperty("html_file")
    private String htmlFile;

    @JsonProperty("rag_text_file")
    private String ragTextFile;
}