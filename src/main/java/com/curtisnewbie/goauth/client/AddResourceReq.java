package com.curtisnewbie.goauth.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yongj.zhuang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddResourceReq {
    private String name;
    private String code;
}
