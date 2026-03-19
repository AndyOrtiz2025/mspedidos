package com.saga.mspedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Map;

@FeignClient(name = "pagos", url = "${pagos.url}")
public interface PagosClient {

    @PostMapping("/pagos/procesar")
    Map<String, Object> procesarPago(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody Map<String, Object> request
    );
}