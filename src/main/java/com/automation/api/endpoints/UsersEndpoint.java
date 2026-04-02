package com.automation.api.endpoints;

import com.automation.api.BaseApiClient;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

// Wraps all REST Assured calls for the /users resource on JSONPlaceholder.
// Each method returns the raw Response so test classes decide what to assert.
// Stateless — safe for parallel test execution.
public class UsersEndpoint extends BaseApiClient {

    private static final String USERS_PATH = "/users";
    private static final String USER_BY_ID = "/users/{id}";

    public Response getAllUsers() {
        return given().spec(BASE_SPEC)
                .when().get(USERS_PATH);
    }

    public Response getUserById(int id) {
        return given().spec(BASE_SPEC)
                .pathParam("id", id)
                .when().get(USER_BY_ID);
    }

    public Response getUserByUsername(String username) {
        return given().spec(BASE_SPEC)
                .queryParam("username", username)
                .when().get(USERS_PATH);
    }

    public Response deleteUser(int id) {
        return given().spec(BASE_SPEC)
                .pathParam("id", id)
                .when().delete(USER_BY_ID);
    }
}
