package com.saga.mspedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "notificaciones", url = "${notificaciones.url}")
public interface NotificacionesClient {

    @PostMapping("/notificaciones/enviar")
    Map<String, Object> enviarNotificacion(@RequestBody Map<String, Object> request);
}