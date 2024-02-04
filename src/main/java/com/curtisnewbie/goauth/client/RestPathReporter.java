package com.curtisnewbie.goauth.client;

import com.curtisnewbie.common.vo.*;
import lombok.Data;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.env.*;
import org.springframework.util.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Reporter of REST paths parsed by RestPathScanner
 *
 * @author yongj.zhuang
 */
@Slf4j
public class RestPathReporter implements InitializingBean {

    public static final String DISABLE_REPORT_KEY = "goauth.path.report.disabled";

    @Autowired
    private RestPathScanner restPathScanner;
    @Autowired
    private GoAuthClient goAuthClient;
    @Autowired
    private Environment env;

    @Override
    public void afterPropertiesSet() throws Exception {
        final String group = env.getProperty("spring.application.name");

        final boolean disabled = Boolean.parseBoolean(env.getProperty(DISABLE_REPORT_KEY, "false"));
        if (!disabled) {
            restPathScanner.onParsed(restPaths -> {
                CompletableFuture.runAsync(() -> {
                    final Set<String> paths = new HashSet<>();
                    final List<RestPathScanner.RestPath> filtered = restPaths.stream()
                            .filter(p -> {
                                String cp = p.getCompletePath();
                                return !cp.startsWith("/remote") && paths.add((p.httpMethod != null ? p.httpMethod.name() : "") + ":" + cp);
                            })
                            .collect(Collectors.toList());

                    final StopWatch sw = new StopWatch();
                    sw.start();
                    reportResources(filtered, goAuthClient);
                    reportPaths(filtered, group, goAuthClient);
                    sw.stop();
                    log.info("GoAuth RestPath Reported, took: {}ms ({} paths)", sw.getTotalTimeMillis(), filtered.size());
                }).exceptionally(e -> {
                    log.error("Failed to report paths to goauth,", e);
                    return null;
                });
            });
        }
    }

    protected static void reportResources(List<RestPathScanner.RestPath> restPaths, GoAuthClient goAuthClient) {
        final Map<String /* code */, PResource> resources = restPaths.stream()
                .filter(p -> p.pathDoc != null && StringUtils.hasText(p.pathDoc.resCode()))
                .map(p -> new PResource(p.pathDoc.resCode(), p.pathDoc.resName()))
                .collect(Collectors.toMap(r -> r.code, r -> r, (a, b) -> a));

        try {
            resources.forEach((k, v) -> goAuthClient.addResource(new AddResourceReq(v.name, v.code)).assertIsOk());
        } catch (Throwable e) {
            log.error("Failed to report resources to goauth, resources: {}", resources.values(), e);
        }
    }

    protected static void reportPaths(List<RestPathScanner.RestPath> restPaths, String group, GoAuthClient goAuthClient) {
        restPaths.stream()
                .map(p -> {
                    final AddPathReq ar = new AddPathReq();
                    ar.setUrl("/" + group + p.getCompletePath());
                    ar.setGroup(group);
                    ar.setType(p.pathDoc != null ? p.pathDoc.type() : PathType.PROTECTED);
                    ar.setDesc(p.pathDoc != null ? p.pathDoc.description() : "");
                    ar.setResCode(p.pathDoc != null ? p.pathDoc.resCode() : "");
                    ar.setMethod(p.httpMethod.name());
                    return ar;
                })
                .forEach(ar -> reportPath(ar, goAuthClient));
    }

    protected static void reportPath(AddPathReq ar, GoAuthClient goAuthClient) {
        try {
            final Result<Void> res = goAuthClient.addPath(ar);
            if (!res.isOk()) {
                log.error("Failed to report path to goauth, req: {}, error code: {}, error msg: {}",
                        ar, res.getErrorCode(), res.getMsg());
            }
        } catch (Throwable e) {
            log.error("Failed to report path to goauth, req: {}", ar, e);
        }
    }

    @Data
    private static class PResource {
        private String code;
        private String name;

        public PResource(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}

