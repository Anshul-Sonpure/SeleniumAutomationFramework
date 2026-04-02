package com.automation.tests;

import com.automation.api.endpoints.PostsEndpoint;
import com.automation.api.models.Post;
import com.automation.base.BaseApiTest;
import com.automation.utils.ExtentReportManager;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

// Tests for the /posts resource on JSONPlaceholder.
// PostsEndpoint is instantiated inside each @Test method — mirrors the page
// object pattern used in LoginTest and CheckoutFlowTest.
public class PostsApiTest extends BaseApiTest {

    @Test(description = "GET /posts returns 100 posts with status 200",
          groups = {"api", "smoke", "posts"})
    public void testGetAllPosts() {
        PostsEndpoint posts = new PostsEndpoint();
        Response response = posts.getAllPosts();

        ExtentReportManager.getTest().info("GET /posts — status: " + response.getStatusCode());
        response.then().statusCode(200);

        List<Post> postList = response.jsonPath().getList("", Post.class);
        ExtentReportManager.getTest().info("Total posts returned: " + postList.size());

        Assert.assertEquals(postList.size(), 100, "Expected 100 posts from GET /posts");
        Assert.assertNotNull(postList.get(0).getTitle(), "First post title should not be null");
    }

    @Test(description = "GET /posts/1 returns the correct post with status 200",
          groups = {"api", "smoke", "posts"})
    public void testGetPostById() {
        PostsEndpoint posts = new PostsEndpoint();
        Response response = posts.getPostById(1);

        ExtentReportManager.getTest().info("GET /posts/1 — status: " + response.getStatusCode());
        response.then().statusCode(200);

        Post post = response.as(Post.class);
        ExtentReportManager.getTest().info("Post retrieved: " + post);

        Assert.assertEquals(post.getId(), 1, "Post id should be 1");
        Assert.assertEquals(post.getUserId(), 1, "Post userId should be 1");
        Assert.assertNotNull(post.getTitle(), "Post title should not be null");
        Assert.assertFalse(post.getTitle().isEmpty(), "Post title should not be empty");
        Assert.assertNotNull(post.getBody(), "Post body should not be null");
    }

    @Test(description = "GET /posts/9999 returns 404 for a non-existent post",
          groups = {"api", "posts"})
    public void testGetPostByIdNotFound() {
        PostsEndpoint posts = new PostsEndpoint();
        Response response = posts.getPostById(9999);

        ExtentReportManager.getTest().info("GET /posts/9999 — status: " + response.getStatusCode());
        Assert.assertEquals(response.getStatusCode(), 404, "Expected 404 for non-existent post");
    }

    @Test(description = "GET /posts?userId=1 returns only posts authored by userId 1",
          groups = {"api", "posts"})
    public void testGetPostsByUserId() {
        PostsEndpoint posts = new PostsEndpoint();
        Response response = posts.getPostsByUserId(1);

        ExtentReportManager.getTest().info("GET /posts?userId=1 — status: " + response.getStatusCode());
        response.then().statusCode(200);

        List<Post> userPosts = response.jsonPath().getList("", Post.class);
        ExtentReportManager.getTest().info("Posts for userId=1: " + userPosts.size());

        Assert.assertEquals(userPosts.size(), 10, "userId=1 should have exactly 10 posts");
        for (Post post : userPosts) {
            Assert.assertEquals(post.getUserId(), 1,
                    "All returned posts should have userId=1, but found: " + post);
        }
    }

    @Test(description = "POST /posts creates a new post and returns 201 with echoed body",
          groups = {"api", "posts"})
    public void testCreatePost() {
        PostsEndpoint posts = new PostsEndpoint();
        Post newPost = new Post(1, "Test Post Title", "Test post body content");

        ExtentReportManager.getTest().info("Sending POST /posts with: " + newPost);
        Response response = posts.createPost(newPost);
        ExtentReportManager.getTest().info("POST /posts — status: " + response.getStatusCode());

        response.then().statusCode(201);

        Post created = response.as(Post.class);
        ExtentReportManager.getTest().info("Created post: " + created);

        Assert.assertEquals(created.getTitle(), "Test Post Title", "Title should match");
        Assert.assertEquals(created.getBody(), "Test post body content", "Body should match");
        Assert.assertEquals(created.getUserId(), 1, "UserId should match");
        Assert.assertEquals(created.getId(), 101, "JSONPlaceholder assigns id=101 for new posts");
    }

    @Test(description = "PUT /posts/1 fully replaces the post and returns 200",
          groups = {"api", "posts"})
    public void testUpdatePost() {
        PostsEndpoint posts = new PostsEndpoint();
        Post updatedPost = new Post(1, "Updated Title", "Updated body content");

        ExtentReportManager.getTest().info("Sending PUT /posts/1");
        Response response = posts.updatePost(1, updatedPost);
        ExtentReportManager.getTest().info("PUT /posts/1 — status: " + response.getStatusCode());

        response.then().statusCode(200);

        Post returned = response.as(Post.class);
        ExtentReportManager.getTest().info("Updated post: " + returned);

        Assert.assertEquals(returned.getTitle(), "Updated Title", "Title should reflect update");
        Assert.assertEquals(returned.getBody(), "Updated body content", "Body should reflect update");
        Assert.assertEquals(returned.getId(), 1, "Post id should remain 1");
    }

    @Test(description = "PATCH /posts/1 partially updates only the title and returns 200",
          groups = {"api", "posts"})
    public void testPatchPost() {
        PostsEndpoint posts = new PostsEndpoint();
        Post partialUpdate = new Post();
        partialUpdate.setTitle("Patched Title Only");

        ExtentReportManager.getTest().info("Sending PATCH /posts/1 with title only");
        Response response = posts.patchPost(1, partialUpdate);
        ExtentReportManager.getTest().info("PATCH /posts/1 — status: " + response.getStatusCode());

        response.then().statusCode(200);

        Post returned = response.as(Post.class);
        ExtentReportManager.getTest().info("Patched post: " + returned);

        Assert.assertEquals(returned.getTitle(), "Patched Title Only", "Title should reflect patch");
        Assert.assertEquals(returned.getId(), 1, "Post id should remain 1");
    }

    @Test(description = "DELETE /posts/1 returns 200 with an empty body",
          groups = {"api", "posts"})
    public void testDeletePost() {
        PostsEndpoint posts = new PostsEndpoint();

        ExtentReportManager.getTest().info("Sending DELETE /posts/1");
        Response response = posts.deletePost(1);
        ExtentReportManager.getTest().info("DELETE /posts/1 — status: " + response.getStatusCode());

        Assert.assertEquals(response.getStatusCode(), 200, "DELETE should return 200");
        Assert.assertEquals(response.getBody().asString().trim(), "{}",
                "DELETE response body should be an empty JSON object");
    }
}
