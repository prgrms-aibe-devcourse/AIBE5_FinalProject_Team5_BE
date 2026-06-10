package com.bootsignal.domain.institution.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bootsignal.domain.institution.dto.InstitutionResponse;
import com.bootsignal.domain.institution.service.InstitutionService;
import com.bootsignal.global.exception.BootSignalException;
import com.bootsignal.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

@WebMvcTest(controllers = InstitutionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InstitutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstitutionService institutionService;

    @Test
    void getInstitutionReturnsInstitutionResponse() throws Exception {
        InstitutionResponse response = new InstitutionResponse(
                1L, "INST001", "Boot Camp Center", "Seoul, Korea",
                "http://bootcamp.com", "John Doe", "02-123-4567",
                "manager@bootcamp.com", "http://image.url", "We teach Spring",
                LocalDateTime.now(), LocalDateTime.now()
        );

        given(institutionService.getInstitution(1L)).willReturn(response);

        mockMvc.perform(get("/api/institutions/{institutionId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.institutionName").value("Boot Camp Center"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void getInstitutionReturnsNotFoundWhenInstitutionDoesNotExist() throws Exception {
        given(institutionService.getInstitution(999L))
                .willThrow(new BootSignalException(ErrorCode.INSTITUTION_NOT_FOUND));

        mockMvc.perform(get("/api/institutions/{institutionId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INSTITUTION_NOT_FOUND"));
    }
}
