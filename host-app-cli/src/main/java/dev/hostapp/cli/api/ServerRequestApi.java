package dev.hostapp.cli.api;

import dev.hostapp.cli.dto.ServerRequestData;
import dev.hostapp.cli.dto.ServerRequestResponse;
import dev.hostapp.cli.dto.StatisticsResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class ServerRequestApi extends BaseApi {

    public ServerRequestApi(ApiClient client) {
        super(client);
    }

    public UUID create(int cpu, int ram, int disk, String os) throws IOException, InterruptedException {
        return client.post("/requests/create", new ServerRequestData(cpu, ram, disk, os), UUID.class);
    }

    public List<ServerRequestResponse> getAll() throws IOException, InterruptedException {
        return client.getList("/requests", ServerRequestResponse.class);
    }

    public List<ServerRequestResponse> getAllAdmin() throws IOException, InterruptedException {
        return client.getList("/requests/admin", ServerRequestResponse.class);
    }

    public ServerRequestResponse get(UUID id) throws IOException, InterruptedException {
        return client.get("/requests/" + id, ServerRequestResponse.class);
    }

    public ServerRequestResponse update(UUID id, int cpu, int ram, int disk, String os)
            throws IOException, InterruptedException {
        return client.put("/requests/" + id, new ServerRequestData(cpu, ram, disk, os), ServerRequestResponse.class);
    }

    public void delete(UUID id) throws IOException, InterruptedException {
        client.delete("/requests/" + id);
    }

    public ServerRequestResponse updateStatus(UUID id, String status) throws IOException, InterruptedException {
        return client.patch("/requests/" + id + "/status?status=" + encode(status), ServerRequestResponse.class);
    }

    public List<ServerRequestResponse> searchByOs(String os) throws IOException, InterruptedException {
        return client.getList("/requests/search?os=" + encode(os), ServerRequestResponse.class);
    }

    public List<ServerRequestResponse> searchByMinCpu(int minCpu) throws IOException, InterruptedException {
        return client.getList("/requests/search/cpu?minCpu=" + minCpu, ServerRequestResponse.class);
    }

    public List<ServerRequestResponse> filterByStatus(String status) throws IOException, InterruptedException {
        return client.getList("/requests/filter/status?status=" + encode(status), ServerRequestResponse.class);
    }

    public List<ServerRequestResponse> filterByOs(String os) throws IOException, InterruptedException {
        return client.getList("/requests/filter/os?os=" + encode(os), ServerRequestResponse.class);
    }

    public List<ServerRequestResponse> sortBy(String sortBy) throws IOException, InterruptedException {
        return client.getList("/requests/sort?by=" + encode(sortBy), ServerRequestResponse.class);
    }

    public StatisticsResponse getStatistics() throws IOException, InterruptedException {
        return client.get("/requests/statistics", StatisticsResponse.class);
    }

    public byte[] export() throws IOException, InterruptedException {
        return client.getBytes("/requests/export");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
