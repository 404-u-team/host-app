package dev.hostapp.cli.api;

public abstract class BaseApi {

    protected final ApiClient client;

    protected BaseApi(ApiClient client) {
        this.client = client;
    }
}