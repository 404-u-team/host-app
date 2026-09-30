package dev.hostapp.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.hostapp.backend.dto.serverrequests.CreateServerRequestDto;
import dev.hostapp.backend.dto.serverrequests.PatchServerRequestDto;
import dev.hostapp.backend.dto.serverrequests.ServerRequestResponse;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.service.ServerRequestService;
import jakarta.validation.Valid;

@RestController 
@RequestMapping("/servers/requests")
public class ServerRequestController {
    
    private final ServerRequestService serverRequestService;

    public ServerRequestController(
        ServerRequestService serverRequestService 
    ) {
        this.serverRequestService = serverRequestService;
    }

    @GetMapping("")
    public List<ServerRequestResponse> getServerRequests(
        @RequestAttribute("user") User owner
    ) {
        UUID ownerId = owner.getId();
        return serverRequestService.getServerRequestsByOwner(ownerId);
    }

    @PostMapping("")
    public ServerRequestResponse createServerRequest(
        @Valid @RequestBody CreateServerRequestDto req,
        @RequestAttribute("user") User owner
    ) {
        return serverRequestService.createServerRequest(
            req.cpuCores(),
            req.ramGb(),
            req.diskGb(),
            req.os(),
            owner
        );
    }

    @PatchMapping("/{id}")
    public ServerRequestResponse patchServerRequest(
        @PathVariable UUID id,
        @Valid @RequestBody PatchServerRequestDto req,
        @RequestAttribute("user") User owner
    ) {
        return serverRequestService.patchServerRequest(
            id,
            req.cpuCores(),
            req.ramGb(),
            req.diskGb(),
            req.os(),
            req.status(),
            owner
        );
    }
}
