package com.saga.mspedidos.controller;

import com.saga.mspedidos.model.Pedido;
import com.saga.mspedidos.repository.PedidoRepository;
import com.saga.mspedidos.service.OrquestadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final OrquestadorService orquestadorService;
    private final PedidoRepository pedidoRepository;

    public PedidoController(OrquestadorService orquestadorService,
                             PedidoRepository pedidoRepository) {
        this.orquestadorService = orquestadorService;
        this.pedidoRepository = pedidoRepository;
    }

    // Endpoint principal — aqui empieza toda la saga
    @PostMapping
    public ResponseEntity<Pedido> crearPedido(@RequestBody Map<String, Object> request) {
        String producto = request.get("producto").toString();
        Integer cantidad = Integer.parseInt(request.get("cantidad").toString());
        Pedido pedido = orquestadorService.procesarPedido(producto, cantidad);
        return ResponseEntity.ok(pedido);
    }

    // Ver todos los pedidos
    @GetMapping
    public ResponseEntity<List<Pedido>> listar() {
        return ResponseEntity.ok(pedidoRepository.findAll());
    }

    // Ver un pedido por id
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtener(@PathVariable Long id) {
        return pedidoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}