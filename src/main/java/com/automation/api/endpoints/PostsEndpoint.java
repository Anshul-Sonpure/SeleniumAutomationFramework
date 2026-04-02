package com.automation.api.endpoints;

import com.automation.api.BaseApiClient;
import com.automation.api.models.Post;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

// Wraps all REST Assured calls for the /posts resource on JSONPlaceholder.
// Each method returns the raw Response so test classes decide what to assert.
// Stateless — safe for parallel test execution.
public class PostsEndpoint extends BaseApiClient {

    private static final String POSTS_PATH = "/posts";
    private static final String POST_BY_ID = "/posts/{id}";

    public Response getAllPosts() {
        return given().spec(BASE_SPEC)
                .when().get(POSTS_PATH);
    }

    public Response getPostById(int id) {
        return given().spec(BASE_SPEC)
                .pathParam("id", id)
                .when().get(POST_BY_ID);
    }

    public Response getPostsByUserId(int userId) {
        return given().spec(BASE_SPEC)
                .queryParam("userId", userId)
                .when().get(POSTS_PATH);
    }

    public Response createPost(Post post) {
        return given().spec(BASE_SPEC)
                .body(post)
                .when().post(POSTS_PATH);
    }

    public Response updatePost(int id, Post post) {
        return given().spec(BASE_SPEC)
                .pathParam("id", id)
                .body(post)
                .when().put(POST_BY_ID);
    }

    public Response patchPost(int id, Post post) {
        return given().spec(BASE_SPEC)
                .pathParam("id", id)
                .body(post)
                .when().patch(POST_BY_ID);
    }

    public Response deletePost(int id) {
        return given().spec(BASE_SPEC)
                .pathParam("id", id)
                .when().delete(POST_BY_ID);
    }
}
