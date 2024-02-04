package com.curtisnewbie.goauth.client;

import lombok.extern.slf4j.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.cloud.client.discovery.*;
import org.springframework.cloud.openfeign.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author yongj.zhuang
 */
@Slf4j
@EnableDiscoveryClient
@EnableFeignClients
@EnableGoauthPathReport
@SpringBootTest(classes = RestPathReporterTest.class)
@SpringBootApplication
public class RestPathReporterTest {

    @Autowired
    private GoAuthClient goAuthClient;

    @Test
    public void should_report_resources() {
        List<RestPathScanner.RestPath> restPaths = Arrays.asList(
                new RestPathScanner.RestPath("dummy", "dummy-res-path", RequestMethod.GET, new RestPathScanner.PathDocObj(
                        "dummy dummy", PathType.PROTECTED, "dummy-code", "dummy-resource", null
                ))
        );

        RestPathReporter.reportResources(restPaths, goAuthClient);
    }

    @Test
    public void should_report_rest_path() {
        RestPathScanner.PathDocObj doc = new RestPathScanner.PathDocObj();
        List<RestPathScanner.RestPath> restPaths = Arrays.asList(
                new RestPathScanner.RestPath("dummy", "dummy", RequestMethod.GET, doc),
                new RestPathScanner.RestPath("dummy", "info", RequestMethod.GET, doc)
        );

        RestPathReporter.reportPaths(restPaths, "test", goAuthClient);
    }

    @Component
    @RestController
    @RequestMapping("/dummy")
    public static class DummyCtrl {

        @RequestMapping("/any")
        public void any() {
        }

        @GetMapping("/get-info")
        public void getInfo() {
        }

        @PutMapping("/put-info")
        public void putInfo() {
        }

        @PostMapping("/post-info")
        public void postInfo() {
        }

        @DeleteMapping("/del-info")
        public void deleteInfo() {
        }
    }
}
