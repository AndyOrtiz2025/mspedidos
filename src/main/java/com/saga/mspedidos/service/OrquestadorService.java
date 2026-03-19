package com.saga.mspedidos.service;

import com.saga.mspedidos.client.InventarioClient;
import com.saga.mspedidos.client.NotificacionesClient;
import com.saga.mspedidos.client.PagosClient;
import com.saga.mspedidos.model.Pedido;
import com.saga.mspedidos.repository.PedidoRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;

@Service
public class OrquestadorService {

    private final PedidoRepository pedidoRepository;
    private final InventarioClient inventarioClient;
    private final PagosClient pagosClient;
    private final NotificacionesClient notificacionesClient;

    public OrquestadorService(PedidoRepository pedidoRepository,
                               InventarioClient inventarioClient,
                               PagosClient pagosClient,
                               NotificacionesClient notificacionesClient) {
        this.pedidoRepository = pedidoRepository;
        this.inventarioClient = inventarioClient;
        this.pagosClient = pagosClient;
        this.notificacionesClient = notificacionesClient;
    }

    public Pedido procesarPedido(String producto, Integer cantidad) {

        // PASO 1: Crear pedido en estado PENDIENTE
        Pedido pedido = new Pedido(producto, cantidad);
        pedidoRepository.save(pedido);
        System.out.println("[ORQUESTADOR] Pedido #" + pedido.getId() + " creado en estado PENDIENTE");

        // PASO 2: Reservar stock en MS Inventario
        try {
            Map<String, Object> bodyInventario = Map.of(
                "idProducto", producto,
                "cantidad", cantidad
            );
            inventarioClient.reservarStock(bodyInventario);
            System.out.println("[ORQUESTADOR] Stock reservado para producto: " + producto);
        } catch (FeignException e) {
            System.out.println("[ORQUESTADOR] ERROR al reservar stock: " + e.getMessage());
            pedido.setEstado("CANCELADO");
            pedidoRepository.save(pedido);
            return pedido;
        }

        // PASO 3: Procesar pago en MS Pagos (con Idempotency Key)
        String idempotencyKey = UUID.randomUUID().toString();
        boolean pagoAprobado = false;

        try {
            Map<String, Object> bodyPago = Map.of("idPedido", pedido.getId());
            Map<String, Object> respuestaPago = pagosClient.procesarPago(idempotencyKey, bodyPago);
            String statusPago = respuestaPago.get("status").toString();
            pagoAprobado = statusPago.equals("APROBADO");
            System.out.println("[ORQUESTADOR] Resultado del pago: " + statusPago);
        } catch (FeignException e) {
            pagoAprobado = false;
            System.out.println("[ORQUESTADOR] ERROR en pago (excepcion): " + e.getMessage());
        }

        // PASO 4: Decision
        if (pagoAprobado) {
            // CAMINO FELIZ: notificar y completar
            try {
                Map<String, Object> bodyNoti = Map.of(
                    "idPedido", pedido.getId(),
                    "mensaje", "Tu pedido #" + pedido.getId() + " de " + producto + " fue completado exitosamente"
                );
                notificacionesClient.enviarNotificacion(bodyNoti);
                System.out.println("[ORQUESTADOR] Notificacion enviada para pedido #" + pedido.getId());
            } catch (Exception e) {
                // Notificacion fallo pero el pedido igual se completa
                System.out.println("[ORQUESTADOR] AVISO: Notificacion fallo pero pedido se completa igual. Error: " + e.getMessage());
            }

            pedido.setEstado("COMPLETADO");
            pedidoRepository.save(pedido);
            System.out.println("[ORQUESTADOR] Pedido #" + pedido.getId() + " COMPLETADO");

        } else {
            // SAGA: compensar devolviendo el stock
            System.out.println("[ORQUESTADOR] Pago rechazado. Activando compensacion SAGA...");
            try {
                Map<String, Object> bodyLiberar = Map.of(
                    "idProducto", producto,
                    "cantidad", cantidad
                );
                inventarioClient.liberarStock(bodyLiberar);
                System.out.println("[ORQUESTADOR] COMPENSACION ejecutada: stock liberado para " + producto);
            } catch (FeignException e) {
                System.out.println("[ORQUESTADOR] ERROR al liberar stock en compensacion: " + e.getMessage());
            }

            pedido.setEstado("CANCELADO");
            pedidoRepository.save(pedido);
            System.out.println("[ORQUESTADOR] Pedido #" + pedido.getId() + " CANCELADO");
        }

        return pedido;
    }
}