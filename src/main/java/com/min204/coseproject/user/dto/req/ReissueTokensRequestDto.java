package com.min204.coseproject.user.dto.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReissueTokensRequestDto {
    private String email;
    private String refreshToken;
}