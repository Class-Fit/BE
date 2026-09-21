package com.example.classfit.inbody.controller;

import com.example.classfit.common.PageResponse;
import com.example.classfit.inbody.dto.req.InBodyCreateReq;
import com.example.classfit.inbody.dto.res.InBodyAnalyzeRes;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;
import com.example.classfit.inbody.service.InBodyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inbodies")
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
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