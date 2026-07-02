package com.example.incidentintake;

import com.example.incidentintake.common.logging.RequestLoggingFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    RequestLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        MDC.clear();
    }

    @Test
    void doFilter_setsXRequestIdResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/incidents");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
    }

    @Test
    void doFilter_xRequestIdIsValidUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/incidents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader("X-Request-Id");
        assertThat(requestId).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void doFilter_clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/incidents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Confirm MDC is populated during chain execution then cleared after
        final String[] mdcDuringChain = {null};
        FilterChain chain = (req, res) -> mdcDuringChain[0] = MDC.get("requestId");

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringChain[0]).isNotBlank();
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void doFilter_requestIdSetInMdcMatchesResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/incidents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] mdcDuringChain = {null};
        FilterChain chain = (req, res) -> mdcDuringChain[0] = MDC.get("requestId");

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringChain[0]).isEqualTo(response.getHeader("X-Request-Id"));
    }

    @Test
    void doFilter_clearsExistingMdcAfterException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/incidents");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> { throw new RuntimeException("downstream failure"); };

        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException ignored) {
            // expected
        }

        assertThat(MDC.get("requestId")).isNull();
    }
}
