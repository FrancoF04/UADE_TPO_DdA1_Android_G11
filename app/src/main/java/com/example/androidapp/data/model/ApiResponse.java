package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {

    @SerializedName("success")
    private final boolean success;

    @SerializedName("data")
    private final T data;

    @SerializedName("error")
    private final String error;

    @SerializedName("meta")
    private final Meta meta;

    public ApiResponse(boolean success, T data, String error, Meta meta) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.meta = meta;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public Meta getMeta() {
        return meta;
    }

    public static class Meta {

        @SerializedName("total")
        private final int total;

        @SerializedName("page")
        private final int page;

        @SerializedName("limit")
        private final int limit;

        public Meta(int total, int page, int limit) {
            this.total = total;
            this.page = page;
            this.limit = limit;
        }

        public int getTotal() {
            return total;
        }

        public int getPage() {
            return page;
        }

        public int getLimit() {
            return limit;
        }
    }
}
