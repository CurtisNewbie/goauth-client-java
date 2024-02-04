package com.curtisnewbie.goauth.client;

import com.curtisnewbie.common.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * @author yongj.zhuang
 */
@Slf4j
@EnableFeignClients
@SpringBootTest(classes = GoAuthClientTest.class)
@EnableDiscoveryClient
@SpringBootApplication
public class GoAuthClientTest {

    @Autowired
    private GoAuthClient goAuthClient;

    @Test
    public void should_test_resource_access() {
        final TestResAccessReq req = new TestResAccessReq();
        req.setUrl("/goauth/open/api/resource/add");
        req.setRoleNo("role_554107924873216177918");
        req.setMethod("POST");
        final Result<TestResAccessResp> resp = goAuthClient.testResAccess(req);
        resp.assertIsOk();
        final TestResAccessResp r = resp.getData();
        Assertions.assertNotNull(r);
        Assertions.assertTrue(r.isValid());
        log.info("Resp: {}", r);
    }

    @Test
    public void should_add_path() {
        final AddPathReq req = new AddPathReq();
        req.setUrl("/test/url");
        req.setType(PathType.PROTECTED);
        req.setGroup("goauth-client-java");
        final Result<Void> result = goAuthClient.addPath(req);
        result.assertIsOk();
    }

    @Test
    public void should_get_role_info() {
        final RoleInfoReq req = new RoleInfoReq();
        req.setRoleNo("role_554107924873216177918");
        final Result<RoleInfoResp> result = goAuthClient.getRoleInfo(req);
        result.assertIsOk();

        final RoleInfoResp r = result.getData();
        Assertions.assertNotNull(r);
        Assertions.assertTrue(StringUtils.hasText(r.getRoleNo()));
        Assertions.assertTrue(StringUtils.hasText(r.getName()));
        log.info("Resp: {}", r);
    }

}
