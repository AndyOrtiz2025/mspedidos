package com.saga.mspedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "inventario", url = "${inventario.url}")
public interface InventarioClient {

    @PostMapping("/inventario/reservar")
    Map<String, Object> reservarStock(@RequestBody Map<String, Object> request);

    @PostMapping("/inventario/liberar")
    Map<String, Object> liberarStock(@RequestBody Map<String, Object> request);
}