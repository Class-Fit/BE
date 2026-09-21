package com.example.classfit.inbody.controller;

import com.example.classfit.common.PageResponse;
import com.example.classfit.inbody.dto.req.InBodyCreateReq;
import com.example.classfit.inbody.dto.res.InBodyAnalyzeRes;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;
import com.example.classfit.inbody.service.InBodyService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
            @RequestParam Long memberId,
            @RequestBody InBodyCreateReq request
    ) {
        return inBodyService.createInBody(memberId, request);
    }


    @GetMapping
    public PageResponse<InBodyCreateRes> getInBodies(
            @RequestParam Long memberId,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page는 0 이상이어야 합니다.")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 100, message = "size는 100 이하여야 합니다.")
            int size
    ) {
        return inBodyService.getInBodies(
                memberId,
                page,
                size
        );
    }

    @GetMapping("/latest")
    public InBodyCreateRes getLatestInBody(
            @RequestParam Long memberId
    ) {
        return inBodyService.getLatestInBody(memberId);
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