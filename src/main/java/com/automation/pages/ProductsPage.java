package com.automation.pages;

import com.automation.base.BasePage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

// Page Object for the product listing page — verifies load, adds items to cart, navigates to cart.
public class ProductsPage extends BasePage {

    private static final Logger log = LogManager.getLogger(ProductsPage.class);

    @FindBy(className = "title")
    private WebElement pageTitle;

    @FindBy(className = "shopping_cart_link")
    private WebElement shoppingCartIcon;

    public ProductsPage() {
        super();
    }

    public String getPageTitle() {
        return getText(pageTitle);
    }

    public boolean isLoaded() {
        return isDisplayed(pageTitle);
    }

    /**
     * Adds a product to the cart by its visible name.
     * Uses wildcard * and contains(@class) because the name element is an <a> anchor (not a div),
     * and normalize-space handles any surrounding whitespace in the text.
     */
    public void addToCartByName(String productName) {
        By addButton = By.xpath(
            "//div[contains(@class,'inventory_item')]" +
            "[.//*[contains(@class,'inventory_item_name') and normalize-space(.)='" + productName + "']]" +
            "//button[contains(@class,'btn_inventory')]"
        );
        click(addButton);
        log.info("Added to cart: '{}'", productName);
    }

    // Returns 0 if the cart badge is absent (empty cart), otherwise parses the badge count.
    public int getCartCount() {
        try {
            return Integer.parseInt(
                driver.findElement(By.className("shopping_cart_badge")).getText()
            );
        } catch (NoSuchElementException e) {
            return 0;
        }
    }

    public CartPage clickCart() {
        click(shoppingCartIcon);
        log.info("Navigated to Cart page");
        return new CartPage();
    }
}
