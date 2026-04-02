package com.automation.tests;

import com.automation.api.endpoints.UsersEndpoint;
import com.automation.api.models.User;
import com.automation.base.BaseApiTest;
import com.automation.utils.ExtentReportManager;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

// Tests for the /users resource on JSONPlaceholder.
// Also exercises nested POJO deserialization (User.Address and User.Company).
public class UsersApiTest extends BaseApiTest {

    @Test(description = "GET /users returns exactly 10 users with status 200",
          groups = {"api", "smoke", "users"})
    public void testGetAllUsers() {
        UsersEndpoint users = new UsersEndpoint();
        Response response = users.getAllUsers();

        ExtentReportManager.getTest().info("GET /users — status: " + response.getStatusCode());
        response.then().statusCode(200);

        List<User> userList = response.jsonPath().getList("", User.class);
        ExtentReportManager.getTest().info("Total users returned: " + userList.size());

        Assert.assertEquals(userList.size(), 10, "Expected exactly 10 users");

        User first = userList.get(0);
        Assert.assertNotNull(first.getName(),    "First user name should not be null");
        Assert.assertNotNull(first.getEmail(),   "First user email should not be null");
        Assert.assertNotNull(first.getAddress(), "First user address should not be null");
    }

    @Test(description = "GET /users/1 returns Leanne Graham with fully deserialized nested objects",
          groups = {"api", "smoke", "users"})
    public void testGetUserById() {
        UsersEndpoint users = new UsersEndpoint();
        Response response = users.getUserById(1);

        ExtentReportManager.getTest().info("GET /users/1 — status: " + response.getStatusCode());
        response.then().statusCode(200);

        User user = response.as(User.class);
        ExtentReportManager.getTest().info("User retrieved: " + user);

        Assert.assertEquals(user.getId(), 1, "User id should be 1");
        Assert.assertEquals(user.getName(), "Leanne Graham", "User 1 name should be 'Leanne Graham'");
        Assert.assertEquals(user.getUsername(), "Bret", "User 1 username should be 'Bret'");
        Assert.assertTrue(user.getEmail().contains("@"), "Email should contain '@'");

        User.Address address = user.getAddress();
        Assert.assertNotNull(address, "Address should not be null");
        Assert.assertNotNull(address.getCity(), "Address city should not be null");
        Assert.assertFalse(address.getCity().isEmpty(), "Address city should not be empty");

        User.Company company = user.getCompany();
        Assert.assertNotNull(company, "Company should not be null");
        Assert.assertNotNull(company.getName(), "Company name should not be null");

        ExtentReportManager.getTest().info(
                "City: " + address.getCity() + " | Company: " + company.getName());
    }

    @Test(description = "GET /users/9999 returns 404 for a non-existent user",
          groups = {"api", "users"})
    public void testGetUserByIdNotFound() {
        UsersEndpoint users = new UsersEndpoint();
        Response response = users.getUserById(9999);

        ExtentReportManager.getTest().info("GET /users/9999 — status: " + response.getStatusCode());
        Assert.assertEquals(response.getStatusCode(), 404, "Expected 404 for non-existent user");
    }

    @Test(description = "GET /users?username=Bret returns exactly one matching user",
          groups = {"api", "users"})
    public void testGetUserByUsername() {
        UsersEndpoint users = new UsersEndpoint();
        Response response = users.getUserByUsername("Bret");

        ExtentReportManager.getTest().info("GET /users?username=Bret — status: " + response.getStatusCode());
        response.then().statusCode(200);

        List<User> matched = response.jsonPath().getList("", User.class);
        ExtentReportManager.getTest().info("Users matching username=Bret: " + matched.size());

        Assert.assertEquals(matched.size(), 1, "Exactly one user should match username=Bret");
        Assert.assertEquals(matched.get(0).getUsername(), "Bret", "Returned user should be 'Bret'");
        Assert.assertEquals(matched.get(0).getId(), 1, "User 'Bret' should have id=1");
    }

    @Test(description = "DELETE /users/1 returns 200",
          groups = {"api", "users"})
    public void testDeleteUser() {
        UsersEndpoint users = new UsersEndpoint();

        ExtentReportManager.getTest().info("Sending DELETE /users/1");
        Response response = users.deleteUser(1);
        ExtentReportManager.getTest().info("DELETE /users/1 — status: " + response.getStatusCode());

        Assert.assertEquals(response.getStatusCode(), 200, "DELETE should return 200");
        Assert.assertEquals(response.getBody().asString().trim(), "{}",
                "DELETE response body should be an empty JSON object");
    }
}
