package dev.hostapp.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import dev.hostapp.backend.dto.serverrequests.ServerRequestResponse;
import dev.hostapp.backend.exception.ApiException;
import dev.hostapp.backend.model.ServerRequest;
import dev.hostapp.backend.model.User;
import dev.hostapp.backend.repository.ServerRequestRepository;

@Service
public class ServerRequestService {
    private final ServerRequestRepository serverRequestRepository;

    public ServerRequestService(ServerRequestRepository serverRequestRepository) {
        this.serverRequestRepository = serverRequestRepository;
    }

    public List<ServerRequestResponse> getServerRequestsByOwner(UUID ownerId) {
        return serverRequestRepository.findAllByOwnerId(ownerId)
                .stream()
                .map(ServerRequestResponse::from)
                .toList();
    }

    private ServerRequest getServerRequestByOwnerAndId(UUID id, User owner) {
        return serverRequestRepository.findByOwnerIdAndId(owner.getId(), id)
            .orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Server request not found")
        );
    }

    public ServerRequestResponse createServerRequest(
        Integer cpuCores,
        Integer ramGb,
        Integer diskGb,
        ServerRequest.OSType os,
        User owner
    ) {
        ServerRequest serverRequest = new ServerRequest(owner, cpuCores, ramGb, diskGb, os);
        return ServerRequestResponse.from(
            serverRequestRepository.save(serverRequest)
        );
    }

    public ServerRequestResponse patchServerRequest(
        UUID id,
        Integer cpuCores,
        Integer ramGb,
        Integer diskGb,
        ServerRequest.OSType os,
        ServerRequest.RequestStatus status,
        User owner
    ) {
        if (owner.getRole().equals(User.UserRole.ADMIN)) {
            return adminPatchServerRequest(id, status, owner);
        }

        // otherwise it is user
        return userPatchServerRequest(id, cpuCores, ramGb, diskGb, os, status, owner);
    }

    // Patching server request by user role
    private ServerRequestResponse userPatchServerRequest(
        UUID id,
        Integer cpuCores,
        Integer ramGb,
        Integer diskGb,
        ServerRequest.OSType os,
        ServerRequest.RequestStatus status,
        User owner
    ) {
        // checking that there is any changes
        validatePatchNotEmpty(cpuCores, ramGb, diskGb, os, status);

        // user can only change status to be CANCELLED
        if (status != null && status != ServerRequest.RequestStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cant change server request status to any but CANCELLED");
        }

        ServerRequest serverRequest = getServerRequestByOwnerAndId(id, owner);
        
        // user can change request only if its status is CREATED
        if (!serverRequest.getStatus().equals(ServerRequest.RequestStatus.CREATED)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cant change server request because its status is not CREATED");
        }

        return updateAndSaveServerRequest(
            serverRequest,
            cpuCores,
            ramGb,
            diskGb,
            os,
            status
        );
    }

    private ServerRequestResponse adminPatchServerRequest(
        UUID id,
        ServerRequest.RequestStatus status,
        User owner
    ) {
        // checking that there is any changes (вот тут костыль скорее, но нормально)
        validatePatchNotEmpty(null, null, null, null, status);

        // admin can change status to be any but CREATED and CANCELLED
        if (status != null 
            && (
                status == ServerRequest.RequestStatus.CREATED ||
                status == ServerRequest.RequestStatus.CANCELLED
            )) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cant change server request status to any but CREATED and CANCELLED");
        }

        ServerRequest serverRequest = getServerRequestByOwnerAndId(id, owner);
        
        // admin can change request only if its status is not adlredy CANCELLED
        if (serverRequest.getStatus() != ServerRequest.RequestStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cant change server request because its status is CANCELLED");
        }

        return updateAndSaveServerRequest(
            serverRequest, null, null, null, null, status
        );
    }

    private void validatePatchNotEmpty (
        Integer cpuCores,
        Integer ramGb,
        Integer diskGb,
        ServerRequest.OSType os,
        ServerRequest.RequestStatus status
    ) {
        if (cpuCores == null && ramGb == null && diskGb == null && os == null && status == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No changes provided");
        }
    }

    private ServerRequestResponse updateAndSaveServerRequest (
        ServerRequest serverRequest,
        Integer cpuCores,
        Integer ramGb,
        Integer diskGb,
        ServerRequest.OSType os,
        ServerRequest.RequestStatus status
    ) {
        if (cpuCores != null) {
        serverRequest.setCpuCores(cpuCores);
        }

        if (ramGb != null) {
            serverRequest.setRamGb(ramGb);
        }

        if (diskGb != null) {
            serverRequest.setDiskGb(diskGb);
        }

        if (os != null) {
            serverRequest.setOs(os);
        }

        if (status != null) {
            serverRequest.setStatus(status);
        }
        
        return ServerRequestResponse.from(
            serverRequestRepository.save(serverRequest)
        );
    }
}
