package com.example.classfit.inbody.service;

import com.example.classfit.common.PageResponse;
import com.example.classfit.inbody.dto.req.InBodyCreateReq;
import com.example.classfit.inbody.dto.res.InBodyAnalyzeRes;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;
import org.springframework.web.multipart.MultipartFile;

public interface InBodyService {

    InBodyCreateRes createInBody(Long memberId,InBodyCreateReq request);

    PageResponse<InBodyCreateRes> getInBodies(Long memberId, int page, int size);

    InBodyCreateRes getLatestInBody(Long memberId);

    InBodyAnalyzeRes analyzeInBody(MultipartFile image);

    void deleteInBody(Long memberId, Long inBodyId);
}