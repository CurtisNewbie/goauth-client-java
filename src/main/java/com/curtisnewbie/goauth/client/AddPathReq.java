package com.curtisnewbie.goauth.client;

import lombok.Data;

/**
 * @author yongj.zhuang
 */
@Data
public class AddPathReq {
    private PathType type;
    private String url;
    private String method;
    private String group;
    private String desc;
    private String resCode;
}
