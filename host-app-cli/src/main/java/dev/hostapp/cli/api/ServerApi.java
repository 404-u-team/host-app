package dev.hostapp.cli.api;

import dev.hostapp.cli.dto.server.ServerData;
import dev.hostapp.cli.dto.server.ServerResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ServerApi extends BaseApi {

    public ServerApi(ApiClient client) {
        super(client);
    }

    public List<ServerResponse> getAll() throws IOException, InterruptedException {
        return client.getList("/servers", ServerResponse.class);
    }

    public ServerResponse get(UUID id) throws IOException, InterruptedException {
        return client.get("/servers/" + id, ServerResponse.class);
    }

    public ServerResponse update(UUID id, ServerData server) throws IOException, InterruptedException {
        return client.put("/servers/" + id, server, ServerResponse.class);
    }

    public void delete(UUID id) throws IOException, InterruptedException {
        client.delete("/servers/" + id);
    }
}
