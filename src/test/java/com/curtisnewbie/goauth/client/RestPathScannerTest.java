package com.curtisnewbie.goauth.client;

import lombok.extern.slf4j.*;
import org.junit.jupiter.api.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author yongj.zhuang
 */
@Slf4j
//@SpringBootTest(classes = RestPathScannerTest.class)
//@SpringBootApplication
public class RestPathScannerTest {

    @Test
    public void should_parse_rest_path() {
        List<RestPathScanner.RestPath> l = new ArrayList<>();
        RestPathScanner.parseRestPath(l, DummyCtrl.class, t -> t);
        System.out.println(l);
        Assertions.assertEquals(12, l.size()); // 8 (for /any) + 4
        l.forEach(e -> {
            if (e.requestPath.equals("/get-info")) {
                Assertions.assertEquals("dummy-info", e.pathDoc.resCode());
                Assertions.assertEquals("dummy-get-info", e.pathDoc.resName());
            } else {
                Assertions.assertEquals("dummy", e.pathDoc.resCode());
                Assertions.assertEquals("Dummy Resources", e.pathDoc.resName());
            }
        });
    }

    @Test
    public void should_get_complete_path() {
        RestPathScanner.PathDocObj doc = new RestPathScanner.PathDocObj();
        Assertions.assertEquals("/dummy", new RestPathScanner.RestPath("dummy", "", RequestMethod.GET, doc).getCompletePath());
        Assertions.assertEquals("/dummy/info", new RestPathScanner.RestPath("dummy", "info", RequestMethod.GET, doc).getCompletePath());
        Assertions.assertEquals("/info", new RestPathScanner.RestPath("", "/info", RequestMethod.GET, doc).getCompletePath());
        Assertions.assertEquals("/dummy/info", new RestPathScanner.RestPath("dummy", "info/////", RequestMethod.GET, doc).getCompletePath());
    }

    @Component
    @RestController
    @RequestMapping("/dummy")
    @PathDoc(resourceCode = "dummy", resourceName = "Dummy Resources")
    public static class DummyCtrl {

        @RequestMapping("/any")
        public void any() {
        }

        @PathDoc(resourceCode = "dummy-info", resourceName = "dummy-get-info")
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
