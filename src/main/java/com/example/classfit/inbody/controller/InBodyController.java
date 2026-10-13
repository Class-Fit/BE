package com.example.classfit.inbody.controller;

import com.example.classfit.common.PageResponse;
import com.example.classfit.inbody.dto.req.InBodyCreateReq;
import com.example.classfit.inbody.dto.res.InBodyAnalyzeRes;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;
import com.example.classfit.inbody.service.InBodyService;
import com.example.classfit.security.LoginMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inbodies")
@Validated
public class InBodyController {

    private final InBodyService inBodyService;

    @PostMapping
    public InBodyCreateRes createInBody(
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody InBodyCreateReq request
    ) {
        return inBodyService.createInBody(loginMember.getMemberId(), request);
    }


    @GetMapping
    public PageResponse<InBodyCreateRes> getInBodies(
            @AuthenticationPrincipal LoginMember loginMember,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page는 0 이상이어야 합니다.")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 100, message = "size는 100 이하여야 합니다.")
            int size
    ) {
        return inBodyService.getInBodies(
                loginMember.getMemberId(),
                page,
                size
        );
    }

    @GetMapping("/latest")
    public InBodyCreateRes getLatestInBody(
            @AuthenticationPrincipal LoginMember loginMember
    ) {
        return inBodyService.getLatestInBody(loginMember.getMemberId());
    }

    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public InBodyAnalyzeRes analyzeInBody(
            @RequestPart("image") MultipartFile image
    ) {
        return inBodyService.analyzeInBody(image);
    }
}
