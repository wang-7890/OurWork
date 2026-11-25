package com.spring.teamwork.common;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class SettleRemoteClient {
    @Value("${settle.remote.url:http://localhost:8080}")
    private String settleBaseUrl;

    /** 是否启用远程调用（调试时可关闭，避免本地没起甲方服务时报错） */
    @Value("${settle.remote.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public SettleRemoteClient() {
        // 构造 RestTemplate，设置 3s 连接超时 + 3s 读超时
        this.restTemplate = new RestTemplate();
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        restTemplate.setRequestFactory(factory);
    }

    /**
     * 触达甲方结算接口（投票达标场景）。
     *
     * @param contractId 要结算的合约 ID
     * @param result     投票裁决结果（SUCCESS / FAIL），传过去让甲方决定是否跳过已结算的合约
     * @return true=甲方成功受理；false=调用失败或已结算
     */
    public boolean triggerSettle(Long contractId, com.spring.teamwork.model.Enum.Verdict result) {

        if (!enabled) {
            log.warn("[settle] 远程结算已禁用（settle.remote.enabled=false），跳过调用 contractId={}", contractId);
            return false;
        }

        int maxRetries = 2;
        long[] backoffMs = {500, 1000};

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = new HashMap<>();
                body.put("trigger", "vote");
                body.put("caller", "verdict");
                body.put("result", result.name());

                String url = settleBaseUrl + "/api/internal/settle/" + contractId;
                ResponseEntity<String> resp = restTemplate.postForEntity(url,
                        new HttpEntity<>(body, headers), String.class);

                // HTTP 2xx + 业务 code=0 → 成功
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null && resp.getBody().contains("\"code\":0")) {
                    log.info("[settle] 远程结算成功 contractId={} result={} attempt={}", contractId, result, attempt);
                    return true;
                }

                log.warn("[settle] 远程结算业务失败 contractId={} attempt={} body={}",
                        contractId, attempt, resp.getBody());

            } catch (Exception e) {
                log.warn("[settle] 远程结算网络异常 contractId={} attempt={} msg={}",
                        contractId, attempt, e.getMessage());
            }

            // 退避（最后一次不 sleep）
            if (attempt < maxRetries) {
                try { Thread.sleep(backoffMs[attempt]); } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 全部失败 → 打 ERROR 日志 + 告警（预留告警钩子，MVP 先只打日志）
        log.error("[settle] 远程结算全部失败 contractId={} result={} 已连续重试 {} 次，等定时任务兜底",
                contractId, result, maxRetries + 1);
        // alertOnSettleFailure(contractId, result);  // TODO 告警接入
        return false;
    }

}
