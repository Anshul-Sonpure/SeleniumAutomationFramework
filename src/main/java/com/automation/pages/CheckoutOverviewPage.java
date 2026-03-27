package com.automation.pages;

import com.automation.base.BasePage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CheckoutOverviewPage — Page Object for the order review screen (/checkout-step-two.html).
 *
 * This is the second (and final) step before placing the order.
 * The user reviews their items, sees the price breakdown, and either finishes or cancels.
 */
public class CheckoutOverviewPage extends BasePage {

    private static final Logger log = LogManager.getLogger(CheckoutOverviewPage.class);

    /**
     * "Finish" button — confirms the order and navigates to the confirmation page.
     * Using data-test attribute for stability.
     */
    @FindBy(css = "[data-test='finish']")
    private WebElement finishButton;

    /**
     * The total amount label that shows the final price including tax.
     * Text format: "Total: $XX.XX"
     */
    @FindBy(className = "summary_total_label")
    private WebElement orderTotalLabel;

    /**
     * The subtotal label showing item prices before tax.
     * Text format: "Item total: $XX.XX"
     */
    @FindBy(className = "summary_subtotal_label")
    private WebElement subtotalLabel;

    /**
     * The tax label.
     * Text format: "Tax: $X.XX"
     */
    @FindBy(className = "summary_tax_label")
    private WebElement taxLabel;

    public CheckoutOverviewPage() {
        super();
    }

    // -------------------------------------------------------------------------
    // Order summary getters
    // -------------------------------------------------------------------------

    /**
     * Returns the names of all products shown in the order overview.
     * Used to verify the correct items carried through from the cart to checkout.
     *
     * Finds all inventory_item_name elements on the overview page and maps
     * them to their text content using Java Streams.
     *
     * @return  list of product names in the order (e.g. ["Sauce Labs Backpack", ...])
     */
    public List<String> getOrderedItemNames() {
        return driver.findElements(By.className("inventory_item_name"))
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    /**
     * Returns the full order total text (including "Total: " prefix).
     * Example: "Total: $43.18"
     *
     * @return  order total string shown on the overview page
     */
    public String getOrderTotal() {
        return getText(orderTotalLabel);
    }

    /**
     * Returns the item subtotal text before tax is applied.
     * Example: "Item total: $40.00"
     *
     * @return  subtotal string
     */
    public String getSubtotal() {
        return getText(subtotalLabel);
    }

    /**
     * Returns the tax amount string.
     * Example: "Tax: $3.20"
     *
     * @return  tax string
     */
    public String getTax() {
        return getText(taxLabel);
    }

    // -------------------------------------------------------------------------
    // Navigation actions
    // -------------------------------------------------------------------------

    /**
     * Clicks the Finish button to confirm and place the order.
     * Navigates to the order confirmation page.
     *
     * @return  OrderConfirmationPage — the page shown after order is placed
     */
    public OrderConfirmationPage clickFinish() {
        log.info("Order total: {} — clicking Finish to place order", getOrderTotal());
        click(finishButton);
        return new OrderConfirmationPage();
    }
}
