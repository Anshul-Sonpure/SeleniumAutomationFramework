package com.automation.tests;

import com.automation.base.BaseTest;
import com.automation.pages.*;
import com.automation.utils.ConfigReader;
import com.automation.utils.ExtentReportManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

// End-to-end test covering the complete purchase journey on saucedemo.com.
public class CheckoutFlowTest extends BaseTest {

    // Constants used in both add-to-cart and order verification steps.
    private static final String PRODUCT_1 = "Sauce Labs Backpack";
    private static final String PRODUCT_2 = "Sauce Labs Bike Light";

    @Test(description = "End-to-end: login, add to cart, checkout, confirm and save order ID",
          groups = {"smoke", "checkout"})
    public void testCompleteCheckoutFlow() {

        // Page objects created inside the @Test method (not @BeforeMethod) to guarantee
        // BaseTest.setUp() has already initialised the driver before any page is constructed.

        ExtentReportManager.getTest().info("Step 1: Logging in");
        LoginPage loginPage = new LoginPage();
        ProductsPage productsPage = loginPage.login(
                ConfigReader.get("app.username", "standard_user"),
                ConfigReader.get("app.password", "secret_sauce"));
        Assert.assertEquals(productsPage.getPageTitle(), "Products",
                "Step 1 FAILED — did not land on Products page after login");
        ExtentReportManager.getTest().info("Step 1 PASSED: Landed on Products page");

        ExtentReportManager.getTest().info("Step 2: Adding items to cart");
        productsPage.addToCartByName(PRODUCT_1);
        productsPage.addToCartByName(PRODUCT_2);
        int cartCount = productsPage.getCartCount();
        ExtentReportManager.getTest().info("Cart badge count: " + cartCount);
        Assert.assertEquals(cartCount, 2,
                "Step 2 FAILED — expected 2 items in cart badge, got: " + cartCount);
        ExtentReportManager.getTest().info("Step 2 PASSED: 2 items added to cart");

        ExtentReportManager.getTest().info("Step 3: Opening cart");
        CartPage cartPage = productsPage.clickCart();
        List<String> cartItemNames = cartPage.getCartItemNames();
        ExtentReportManager.getTest().info("Items in cart: " + cartItemNames);
        int cartItemCount = cartPage.getCartItemCount();
        Assert.assertEquals(cartItemCount, 2,
                "Step 3 FAILED — expected 2 items in cart, got: " + cartItemCount);
        Assert.assertTrue(cartItemNames.contains(PRODUCT_1), "Step 3 FAILED — cart missing: " + PRODUCT_1);
        Assert.assertTrue(cartItemNames.contains(PRODUCT_2), "Step 3 FAILED — cart missing: " + PRODUCT_2);
        ExtentReportManager.getTest().info("Step 3 PASSED: Cart contains correct items");

        ExtentReportManager.getTest().info("Step 4: Proceeding to checkout");
        CheckoutInfoPage checkoutInfoPage = cartPage.clickCheckout();
        ExtentReportManager.getTest().info("Step 4 PASSED: On checkout info page");

        ExtentReportManager.getTest().info("Step 5: Filling checkout details");
        CheckoutOverviewPage overviewPage = checkoutInfoPage.fillAndContinue("John", "Doe", "10001");
        ExtentReportManager.getTest().info("Step 5 PASSED: Checkout details submitted");

        ExtentReportManager.getTest().info("Step 6: Reviewing order summary");
        List<String> orderedItems = overviewPage.getOrderedItemNames();
        String subtotal    = overviewPage.getSubtotal();
        String tax         = overviewPage.getTax();
        String orderTotal  = overviewPage.getOrderTotal();
        ExtentReportManager.getTest().info("Items: " + orderedItems + " | Subtotal: " + subtotal + " | Tax: " + tax + " | Total: " + orderTotal);
        Assert.assertTrue(orderedItems.contains(PRODUCT_1), "Step 6 FAILED — overview missing: " + PRODUCT_1);
        Assert.assertTrue(orderedItems.contains(PRODUCT_2), "Step 6 FAILED — overview missing: " + PRODUCT_2);
        ExtentReportManager.getTest().info("Step 6 PASSED: Order summary verified");

        ExtentReportManager.getTest().info("Step 7: Placing order");
        OrderConfirmationPage confirmationPage = overviewPage.clickFinish();

        ExtentReportManager.getTest().info("Step 8: Verifying order confirmation");
        String confirmationHeader = confirmationPage.getConfirmationHeader();
        ExtentReportManager.getTest().info("Confirmation: " + confirmationHeader);
        Assert.assertEquals(confirmationHeader, "Thank you for your order!",
                "Step 8 FAILED — order confirmation message did not match");

        String orderId = confirmationPage.saveOrderDetails(orderTotal, orderedItems);
        ExtentReportManager.getTest().info("Order ID: " + orderId);
        Assert.assertNotNull(orderId, "Order ID should not be null");
        ExtentReportManager.getTest().info("CHECKOUT FLOW COMPLETE — Order ID: " + orderId + " | Total: " + orderTotal);
    }
}
