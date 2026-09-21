package com.example.classfit.inbody.service;

import com.example.classfit.common.PageResponse;
import com.example.classfit.common.exception.BusinessException;
import com.example.classfit.inbody.domain.InBody;
import com.example.classfit.inbody.dto.req.InBodyCreateReq;
import com.example.classfit.inbody.dto.res.InBodyAnalyzeRes;
import com.example.classfit.inbody.dto.res.InBodyCreateRes;
import com.example.classfit.inbody.exception.InBodyErrorCode;
import com.example.classfit.inbody.repository.InBodyRepository;
import com.example.classfit.member.domain.Member;
import com.example.classfit.member.repository.MemberRepository;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InBodyServiceImpl implements InBodyService {

    private final InBodyRepository inBodyRepository;
    private final MemberRepository memberRepository;
    private final OpenAIClient openAIClient;


    @Value("classpath:prompts/inbody/inbody-analysis.txt")
    private Resource inBodyPromptResource;


    @Override
    @Transactional
    public InBodyCreateRes createInBody(Long memberId, InBodyCreateReq request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(InBodyErrorCode.MEMBER_NOT_FOUND)
                );

        InBody inBody = InBody.builder()
                .member(member)
                .heightCm(request.heightCm())
                .weightKg(request.weightKg())
                .bodyFatPercentage(request.bodyFatPercentage())
                .skeletalMuscleMassKg(request.skeletalMuscleMassKg())
                .bodyFatMassKg(request.bodyFatMassKg())
                .bmi(request.bmi())
                .build();

        InBody savedInBody = inBodyRepository.save(inBody);

        return toResponse(savedInBody);
    }

    @Override
    public PageResponse<InBodyCreateRes> getInBodies(Long memberId, int page, int size) {

        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(InBodyErrorCode.MEMBER_NOT_FOUND);
        }


        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<InBodyCreateRes> result =
                inBodyRepository.findAllByMemberId(memberId, pageable)
                        .map(this::toResponse);

        return PageResponse.from(result);
    }

    @Override
    public InBodyCreateRes getLatestInBody(Long memberId) {

        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(InBodyErrorCode.MEMBER_NOT_FOUND);
        }

        InBody inBody = inBodyRepository
                .findTopByMemberIdOrderByCreatedAtDesc(memberId)
                .orElseThrow(() -> new BusinessException(InBodyErrorCode.INBODY_NOT_FOUND)
                );

        return toResponse(inBody);
    }

    private InBodyCreateRes toResponse(InBody inBody) {

        return new InBodyCreateRes(
                inBody.getId(),
                inBody.getHeightCm(),
                inBody.getWeightKg(),
                inBody.getBodyFatPercentage(),
                inBody.getSkeletalMuscleMassKg(),
                inBody.getBodyFatMassKg(),
                inBody.getBmi(),
                inBody.getCreatedAt()
        );
    }

    @Override
    public InBodyAnalyzeRes analyzeInBody(MultipartFile image) {
        validateImage(image);
        try {
            String prompt = inBodyPromptResource.getContentAsString(
                    StandardCharsets.UTF_8
            );

            String mimeType = image.getContentType();

            String base64Image = Base64.getEncoder()
                    .encodeToString(image.getBytes());

            String imageUrl =
                    "data:" + mimeType + ";base64," + base64Image;

            List<ResponseInputContent> contents = List.of(
                    ResponseInputContent.ofInputText(
                            ResponseInputText.builder()
                                    .text(prompt)
                                    .build()
                    ),
                    ResponseInputContent.ofInputImage(
                            ResponseInputImage.builder()
                                    .imageUrl(imageUrl)
                                    .detail(ResponseInputImage.Detail.HIGH)
                                    .build()
                    )
            );

            ResponseInputItem input =
                    ResponseInputItem.ofEasyInputMessage(
                            EasyInputMessage.builder()
                                    .role(EasyInputMessage.Role.USER)
                                    .content(
                                            EasyInputMessage.Content
                                                    .ofResponseInputMessageContentList(
                                                            contents
                                                    )
                                    )
                                    .build()
                    );

            StructuredResponseCreateParams<InBodyAnalyzeRes> params =
                    ResponseCreateParams.builder()
                            .model(ChatModel.GPT_5_2)
                            .inputOfResponse(List.of(input))
                            .text(InBodyAnalyzeRes.class)
                            .build();

            StructuredResponse<InBodyAnalyzeRes> response =
                    openAIClient.responses().create(params);

            return response.output().stream()
                    .flatMap(output -> output.message().stream())
                    .flatMap(message -> message.content().stream())
                    .flatMap(content -> content.outputText().stream())
                    .findFirst()
                    .orElseThrow(() ->
                            new BusinessException(
                                    InBodyErrorCode.IMAGE_ANALYSIS_FAILED
                            )
                    );

        } catch (IOException e) {
            throw new BusinessException(
                    InBodyErrorCode.IMAGE_ANALYSIS_FAILED
            );
        }
    }

    private void validateImage(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new BusinessException(
                    InBodyErrorCode.IMAGE_REQUIRED
            );
        }

        String contentType = image.getContentType();

        if (contentType == null ||
                !contentType.startsWith("image/")) {

            throw new BusinessException(
                    InBodyErrorCode.INVALID_IMAGE_TYPE
            );
        }
    }
}