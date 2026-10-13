package com.example.classfit.inbody.controller;

import com.example.classfit.inbody.dto.req.InBodyCreateReq;
import com.example.classfit.inbody.service.InBodyService;
import com.example.classfit.security.LoginMember;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

class InBodyControllerTest {

    private final InBodyService service = mock(InBodyService.class);
    private final InBodyController controller = new InBodyController(service);
    private final LoginMember member = mock(LoginMember.class);

    @Test
    void usesAuthenticatedMemberForSavingAndReadingInBody() {
        when(member.getMemberId()).thenReturn(42L);
        InBodyCreateReq values = new InBodyCreateReq(
                new BigDecimal("170"), new BigDecimal("65"), new BigDecimal("20"),
                new BigDecimal("28"), new BigDecimal("13"), new BigDecimal("22.5"));

        controller.createInBody(member, values);
        controller.getInBodies(member, 0, 10);
        controller.getLatestInBody(member);

        verify(service).createInBody(42L, values);
        verify(service).getInBodies(42L, 0, 10);
        verify(service).getLatestInBody(42L);
    }
}
