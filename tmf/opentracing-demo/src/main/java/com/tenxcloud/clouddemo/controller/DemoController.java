/*
 * Licensed Materials - Property of tenxcloud.com
 * (C) Copyright 2018 TenxCloud. All Rights Reserved.
 */

package com.tenxcloud.clouddemo.controller;

import com.tenxcloud.clouddemo.feign.demo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 2018/7/19 @author wuhuhu
 */
@RestController
@RequestMapping("/demo")
@RefreshScope
@Slf4j
public class DemoController {


    private static Map<String, String> urlMap = new HashMap<>();
    private static String ENV = "env";
    private static String TIMEOUT = "timeout";

    static {
        urlMap.put(TIMEOUT, "http://%s/demo/timeout/%s");
        urlMap.put(ENV, "http://%s/demo/env/%s");
    }

    @Autowired
    RestTemplate balancedRestTemplate;
    @Value("${project.version}")
    private String value;
    @Autowired
    private DemoBFeignClient bFeignClient;
    @Autowired
    private DemoCFeignClient cFeignClient;
    @Autowired
    private DemoDFeignClient dFeignClient;
    @Autowired
    private DemoEFeignClient eFeignClient;
    @Autowired
    private DemoFFeignClient fFeignClient;
    private AtomicInteger counter = new AtomicInteger(0);
    private String service = "opentracing-demo-%s";

    @GetMapping("call/{id}/{method}/{msg}")
    public String ribbonTimeout(@PathVariable String id, @PathVariable String method, @PathVariable int msg) {
        String serviceName = String.format(service, id);
        String url = urlMap.getOrDefault(method, urlMap.get(ENV));
        URI uri = URI.create(String.format(url, serviceName, msg));
        log.info("###request url: {}###", uri.toString());
        return balancedRestTemplate.getForObject(uri, String.class);
    }


    @GetMapping("/timeout/{duration}")
    public String timeout(@PathVariable int duration) {
        try {
            log.info("超时时间: {}，当前调用次数：{}", duration, counter.incrementAndGet());
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (counter.get() == 1000) {
            counter.set(1);
        }
        MDC.put("mdc_trace_id", "traceId");
        return String.format("success, sleep: %s毫秒, 当前调用次数：%s", duration, counter.get());
    }

    @GetMapping("/headers")
    public Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        Enumeration<String> headers = request.getHeaderNames();
        String header;
        while (headers.hasMoreElements()) {
            header = headers.nextElement();
            result.put(header, request.getHeader(header));
        }
        return result;
    }

    @GetMapping("/env/{msg}")
    public String getEnv(@PathVariable String msg) {
        HttpStatus[] codes = HttpStatus.values();
        HttpStatus code;
        for (int i = 0; i < codes.length; i++) {
            code = codes[i];
            if (code.isError() && String.valueOf(code.value()).equals(msg)) {
                throw new RuntimeException("错误码：" + msg);
            }
        }
        String r = System.getenv(msg.toUpperCase());
        return StringUtils.isNotBlank(r) ? r : msg;
    }

    @GetMapping("/b/env/{msg}")
    public String getBEnv(@PathVariable String msg) {
        return bFeignClient.getEnv(msg);
    }

    @GetMapping("/c/env/{msg}")
    public String getCEnv(@PathVariable String msg) {
        return cFeignClient.getEnv(msg);
    }

    @GetMapping("/d/env/{msg}")
    public String getDEnv(@PathVariable String msg) {
        return dFeignClient.getEnv(msg);
    }

    @GetMapping("/e/env/{msg}")
    public String getEEnv(@PathVariable String msg) {
        return eFeignClient.getEnv(msg);
    }

    @GetMapping("/f/env/{msg}")
    public String getFEnv(@PathVariable String msg) {
        return fFeignClient.getEnv(msg);
    }


    @GetMapping("/refresh/value")
    public String refresh() {
        return value;
    }

    @GetMapping("/b/refresh/value")
    public String bRefresh() {
        return bFeignClient.refresh();
    }

    @GetMapping("/c/refresh/value")
    public String cRefresh() {
        return cFeignClient.refresh();
    }

    @GetMapping("/d/refresh/value")
    public String dRefresh() {
        return dFeignClient.refresh();
    }

    @GetMapping("/e/refresh/value")
    public String eRefresh() {
        return eFeignClient.refresh();
    }

    @GetMapping("/f/refresh/value")
    public String fRefresh() {
        return fFeignClient.refresh();
    }

}
