package com.automation.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Post {

    @JsonProperty("userId")
    private int userId;

    @JsonProperty("id")
    private int id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;

    public Post() {}

    public Post(int userId, String title, String body) {
        this.userId = userId;
        this.title  = title;
        this.body   = body;
    }

    public int    getUserId()            { return userId; }
    public void   setUserId(int userId)  { this.userId = userId; }

    public int    getId()                { return id; }
    public void   setId(int id)          { this.id = id; }

    public String getTitle()             { return title; }
    public void   setTitle(String title) { this.title = title; }

    public String getBody()              { return body; }
    public void   setBody(String body)   { this.body = body; }

    @Override
    public String toString() {
        return "Post{id=" + id + ", userId=" + userId
             + ", title='" + title + "', body='" + body + "'}";
    }
}
