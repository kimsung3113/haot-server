package com.haot.coupon.application.dto.request.events;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "이벤트 검색 REQUEST DTO")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventSearchRequest {

    // 유저가 보낼 수 있는 것 (삭제되지 않은, start date < 현재 < end Date, 이벤트 상태값이 DEFAULT인 이벤트만 조회)
    @Schema(description = "유저가 보낼 수 있는 이름 검색 키워드")
    private String nameKeyword;

    @Schema(description = "유저가 보낼 수 있는 이벤트 설명 검색 키워드")
    private String descriptionKeyword;

    // 나머지는 다 Admin만 보낼 수 있게끔
    @Schema(description = "Admin만 가능, 이벤트 시작일 기준")
    private LocalDateTime startDate;

    @Schema(description = "Admin만 가능, 이벤트 종료일 기준")
    private LocalDateTime endDate;

    @Schema(description = "Admin만 가능, 삭제 여부")
    private Boolean isDelete;   // 삭제된 이벤트 여부

    @Schema(description = "DEFAULT, MANUALLY_CLOSED, EXPIRED, OUT_OF_STOCK 만 요청 가능합니다.")
    @Pattern(regexp = "DEFAULT|MANUALLY_CLOSED|EXPIRED|OUT_OF_STOCK", message = "유효한 상태 값을 입력하세요.")
    private String eventStatus;

    public boolean isAllFieldsNull() {
        return nameKeyword == null &&
                descriptionKeyword == null &&
                startDate == null &&
                endDate == null &&
                isDelete == null &&
                eventStatus == null;
    }
}
