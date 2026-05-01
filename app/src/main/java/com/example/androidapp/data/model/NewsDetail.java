package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class NewsDetail extends News {

    @SerializedName("content")
    private String content;

    public String getContent() { return content; }
}
