package com.lacy.chat.modules.user.dto.req;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserRequest {


    private String email;

    private String name;
    private String picture;
    private String provider; // GOOGLE, GITHUB
    private String providerId;
}
