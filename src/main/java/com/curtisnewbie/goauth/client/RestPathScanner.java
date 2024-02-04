package com.curtisnewbie.goauth.client;

import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.aop.support.*;
import org.springframework.beans.*;
import org.springframework.context.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.*;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import static java.util.Collections.*;

/**
 * Scanner of REST Path
 * <p>
 * Potential candidates include beans that are annotated with @Controller and @RestController
 *
 * @author yongj.zhuang
 */
@Slf4j
public class RestPathScanner implements ApplicationContextAware {

    private static final Map<Class<? extends Annotation>, MappingPathParser> clz2Parser = new HashMap<>();
    private volatile List<RestPath> parsedRestPaths = new ArrayList<>();
    private List<Consumer<List<RestPath>>> onParsed = new ArrayList<>();

    static {
        clz2Parser.put(RequestMapping.class, o -> {
            RequestMapping rm = (RequestMapping) o;
            RequestMethod method = rm.method().length > 0 ? rm.method()[0] : null;

            String path;
            if (rm.value().length > 0) path = rm.value()[0];
            else path = rm.path().length > 0 ? rm.path()[0] : "";

            if (method != null) {
                return singletonList(new ParsedMapping(path, method));
            }

            // all HTTP methods
            final List<ParsedMapping> parsed = new ArrayList<>();
            for (RequestMethod mtd : RequestMethod.values()) {
                parsed.add(new ParsedMapping(path, mtd));
            }
            return parsed;
        });
        clz2Parser.put(GetMapping.class, o -> {
            GetMapping gm = (GetMapping) o;
            RequestMethod method = RequestMethod.GET;
            if (gm.value().length > 0) return singletonList(new ParsedMapping(gm.value()[0], method));
            return singletonList(new ParsedMapping(gm.path().length > 0 ? gm.path()[0] : "", method));
        });
        clz2Parser.put(PutMapping.class, o -> {
            PutMapping pm = (PutMapping) o;
            RequestMethod method = RequestMethod.PUT;
            if (pm.value().length > 0) return singletonList(new ParsedMapping(pm.value()[0], method));
            return singletonList(new ParsedMapping(pm.path().length > 0 ? pm.path()[0] : "", method));
        });
        clz2Parser.put(PostMapping.class, o -> {
            PostMapping pm = (PostMapping) o;
            RequestMethod method = RequestMethod.POST;
            if (pm.value().length > 0) return singletonList(new ParsedMapping(pm.value()[0], method));
            return singletonList(new ParsedMapping(pm.path().length > 0 ? pm.path()[0] : "", method));
        });
        clz2Parser.put(DeleteMapping.class, o -> {
            DeleteMapping dm = (DeleteMapping) o;
            RequestMethod method = RequestMethod.DELETE;
            if (dm.value().length > 0) return singletonList(new ParsedMapping(dm.value()[0], method));
            return singletonList(new ParsedMapping(dm.path().length > 0 ? dm.path()[0] : "", method));
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
        final Map<String, Object> beans = appCtx.getBeansWithAnnotation(Controller.class);
        final List<RestPath> restPaths = new ArrayList<>();

        final StopWatch sw = new StopWatch();
        sw.start();
        beans.forEach((k, v) -> parseRestPath(restPaths, AopUtils.getTargetClass(v), appCtx.getEnvironment()::resolvePlaceholders));
        sw.stop();

        log.info("GoAuth RestPath Scanned, found: {} REST paths, took: {}ms", restPaths.size(), sw.getTotalTimeMillis());

        synchronized (this) {
            this.parsedRestPaths = restPaths;
            if (!this.onParsed.isEmpty()) {
                this.onParsed.forEach(callback -> {
                    callback.accept(new ArrayList<>(this.parsedRestPaths));
                });
            }
        }
    }

    /** Register onParsed callback */
    public void onParsed(Consumer<List<RestPath>> callback) {
        if (callback == null) return;
        synchronized (this) {
            if (this.parsedRestPaths != null) {
                callback.accept(new ArrayList<>(this.parsedRestPaths));
            } else {
                this.onParsed.add(callback);
            }
        }
    }

    public static PathDoc extractDoc(Method m) {
        for (Annotation mda : m.getDeclaredAnnotations()) {
            Class<?> typ = mda.annotationType();
            if (PathDoc.class.equals(typ)) {
                return ((PathDoc) mda);
            }
        }
        return null;
    }

    public static void parseRestPath(List<RestPath> restPathList, Class<?> beanClz, Function<String, String> resolvePlaceholders) {
        String rootPath = "";
        final RequestMapping rootMapping = beanClz.getDeclaredAnnotation(RequestMapping.class);
        final PathDoc rootDoc = beanClz.getDeclaredAnnotation(PathDoc.class);
        if (rootMapping != null) {
            final List<ParsedMapping> parsed = clz2Parser.get(RequestMapping.class).parsed(rootMapping);
            if (!parsed.isEmpty())
                rootPath = resolvePlaceholders.apply(parsed.get(0).requestPath);
        }

        final Method[] methods = beanClz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            PathDoc pathDoc = extractDoc(m);

            for (Annotation mda : m.getDeclaredAnnotations()) {
                Class<?> typ = mda.annotationType();
                if (clz2Parser.containsKey(typ)) {
                    final List<ParsedMapping> parsed = clz2Parser.get(typ).parsed(mda);
                    for (ParsedMapping pm : parsed) {
                        restPathList.add(new RestPath(rootPath, pm.requestPath, pm.httpMethod, PathDocObj.build(pathDoc, rootDoc)));
                    }

                    break; // normally, a method can only have one mapping
                }
            }
        }
    }

    /**
     * Parsed REST Path, thread-safe
     */
    @ToString
    public static class RestPath {
        public final String rootPath;
        public final String requestPath;
        public final RequestMethod httpMethod;
        @Nullable public final PathDocObj pathDoc;

        public RestPath(String rootPath, String requestPath, RequestMethod httpMethod, @Nullable PathDocObj pathDoc) {
            this.rootPath = rootPath;
            this.requestPath = requestPath;
            this.httpMethod = httpMethod;
            this.pathDoc = pathDoc;
        }

        public String getCompletePath() {
            String rtp = rootPath != null ? rootPath.trim() : "";
            String rqp = requestPath != null ? requestPath.trim() : "";
            if (!rtp.isEmpty() && !rtp.startsWith("/")) rtp = "/" + rtp;
            if (!rqp.isEmpty() && !rqp.startsWith("/")) rqp = "/" + rqp;

            int j = -1;
            for (int i = rqp.length() - 1; i > -1; i--) {
                if (rqp.charAt(i) == '/') j = i;
                else break;
            }
            if (j > -1) rqp = rqp.substring(0, j); // remove trailing '/'


            // not sure about returning "/" like this when both rtp and rqp are empty, doesn't seem like that it will actually happen :D
            /*
                String pt = rtp + rqp;
                return pt.isEmpty() ? "/" : pt;
             */

            return rtp + rqp;
        }
    }

    @Data
    public static class PathDocObj {
        private PathDocObj parentDoc;
        private String description;
        private PathType type;
        private String resCode;
        private String resName;

        public PathDocObj() {}

        public PathDocObj(String description, PathType type, String resCode, String resName, PathDocObj parentDoc) {
            this.description = description;
            this.type = type;
            this.resCode = resCode;
            this.resName = resName;
            this.parentDoc = parentDoc;
        }

        public static PathDocObj build(PathDoc doc) {
            if (doc == null) {return null;}
            return new PathDocObj(doc.description(), doc.type(), doc.resourceCode(), doc.resourceName(), null);
        }

        public static PathDocObj build(PathDoc doc, PathDoc parent) {
            if (doc == null) {return build(parent);}
            return new PathDocObj(doc.description(), doc.type(), doc.resourceCode(), doc.resourceName(), parent != null ? build(parent) : null);
        }

        public String description() {
            String s = description;
            if (StringUtils.hasText(s)) return s;
            if (parentDoc != null && StringUtils.hasText(s = parentDoc.description())) return s;
            if (s == null) s = "";
            return s;
        }

        public PathType type() {
            PathType t = type;
            if (t != null) return t;
            if (parentDoc != null && (t = parentDoc.type()) != null) return t;
            if (t == null) t = PathType.PROTECTED;
            return t;
        }

        public String resCode() {
            String code = resCode;
            if (StringUtils.hasText(code)) return code;
            if (parentDoc != null && StringUtils.hasText(code = parentDoc.resCode())) return code;
            if (code == null) code = "";
            return code;
        }

        public String resName() {
            String name = resName;
            if (StringUtils.hasText(name)) return name;
            if (parentDoc != null && StringUtils.hasText(name = parentDoc.resName())) return name;
            if (name == null) name = "";
            return name;
        }
    }

    @FunctionalInterface
    private interface MappingPathParser {
        List<ParsedMapping> parsed(Annotation o);
    }

    @AllArgsConstructor
    public static class ParsedMapping {
        public final String requestPath;
        public final RequestMethod httpMethod;
    }
}
