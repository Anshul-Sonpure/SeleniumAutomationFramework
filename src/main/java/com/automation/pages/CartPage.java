package com.automation.pages;

import com.automation.base.BasePage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.stream.Collectors;

// Page Object for the shopping cart — verifies cart contents and navigates to checkout.
public class CartPage extends BasePage {

    private static final Logger log = LogManager.getLogger(CartPage.class);

    @FindBy(className = "cart_item")
    private List<WebElement> cartItems;

    // data-test attribute locators are stable across UI redesigns.
    @FindBy(css = "[data-test='checkout']")
    private WebElement checkoutButton;

    @FindBy(css = "[data-test='continue-shopping']")
    private WebElement continueShoppingButton;

    public CartPage() {
        super();
    }

    public int getCartItemCount() {
        wait.forVisible(By.cssSelector(".cart_list"));
        return driver.findElements(By.className("cart_item")).size();
    }

    public List<String> getCartItemNames() {
        return driver.findElements(By.className("inventory_item_name"))
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public CheckoutInfoPage clickCheckout() {
        click(checkoutButton);
        log.info("Navigated to Checkout Info page");
        return new CheckoutInfoPage();
    }
}
