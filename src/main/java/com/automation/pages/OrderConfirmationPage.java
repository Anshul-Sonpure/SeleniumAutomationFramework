package com.automation.pages;

import com.automation.base.BasePage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * OrderConfirmationPage — Page Object for the order success screen (/checkout-complete.html).
 *
 * This is the final page of the checkout flow.
 * It confirms that the order was placed and provides a summary for record-keeping.
 *
 * NOTE ON ORDER ID:
 * -----------------
 * saucedemo.com is a demo site and does not generate real order numbers.
 * This framework generates a timestamp-based Order ID (e.g. ORD-20240325-143022)
 * and saves the full order details to test-output/order-confirmation.txt so
 * there is a persistent record of each checkout run.
 */
public class OrderConfirmationPage extends BasePage {

    private static final Logger log = LogManager.getLogger(OrderConfirmationPage.class);

    // File path where all order confirmations are saved (one per run, appended).
    private static final String ORDER_FILE = "test-output/order-confirmation.txt";

    /**
     * The main confirmation heading.
     * Expected text: "Thank you for your order!"
     */
    @FindBy(className = "complete-header")
    private WebElement confirmationHeader;

    /**
     * The supporting text below the heading.
     * Text: "Your order has been dispatched, and will arrive just as fast as the pony can get there!"
     */
    @FindBy(className = "complete-text")
    private WebElement confirmationText;

    /** "Back Home" button — returns to the Products page. */
    @FindBy(css = "[data-test='back-to-products']")
    private WebElement backHomeButton;

    public OrderConfirmationPage() {
        super();
    }

    // -------------------------------------------------------------------------
    // Confirmation state
    // -------------------------------------------------------------------------

    /**
     * Returns the confirmation heading text.
     * Tests should assert this equals "Thank you for your order!" to verify the
     * entire checkout flow completed successfully.
     *
     * @return  confirmation heading string
     */
    public String getConfirmationHeader() {
        return getText(confirmationHeader);
    }

    /**
     * Returns true if the order confirmation heading is visible on the page.
     * Used as a lightweight loaded check before reading text or saving details.
     *
     * @return  true if order was placed and confirmation page is displayed
     */
    public boolean isOrderSuccessful() {
        return isDisplayed(confirmationHeader);
    }

    // -------------------------------------------------------------------------
    // Order persistence
    // -------------------------------------------------------------------------

    /**
     * Generates a pseudo Order ID and saves the full order details to a text file.
     *
     * WHY SAVE TO A FILE?
     * --------------------
     * In real automation suites, order IDs are critical for traceability —
     * a failing order test needs to reference the specific transaction for
     * investigation. Since saucedemo has no real order numbers, we generate
     * a timestamp-based ID and persist all order details alongside it.
     *
     * OUTPUT FORMAT in test-output/order-confirmation.txt:
     * -------------------------------------------------------
     *   ============================
     *   Order ID    : ORD-20240325-143022
     *   Date/Time   : 2024-03-25 14:30:22
     *   Status      : Thank you for your order!
     *   Order Total : Total: $43.18
     *   Items Ordered:
     *     - Sauce Labs Backpack
     *     - Sauce Labs Bike Light
     *   ============================
     *
     * The file is APPENDED (not overwritten) so previous runs are preserved.
     *
     * @param orderTotal    the total amount string from CheckoutOverviewPage
     * @param orderedItems  list of product names from CheckoutOverviewPage
     * @return              the generated Order ID string (e.g. "ORD-20240325-143022")
     */
    public String saveOrderDetails(String orderTotal, List<String> orderedItems) {
        // Generate a unique, human-readable Order ID using the current timestamp.
        String orderId   = "ORD-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String dateTime  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String status    = getConfirmationHeader();

        // Build the order record as a formatted text block.
        StringBuilder record = new StringBuilder();
        record.append("\n============================\n");
        record.append("Order ID    : ").append(orderId).append("\n");
        record.append("Date/Time   : ").append(dateTime).append("\n");
        record.append("Status      : ").append(status).append("\n");
        record.append("Order Total : ").append(orderTotal).append("\n");
        record.append("Items Ordered:\n");
        for (String item : orderedItems) {
            record.append("  - ").append(item).append("\n");
        }
        record.append("============================\n");

        // Write the record to the output file.
        // CREATE creates the file if absent; APPEND adds to it if it already exists
        // so that multiple test runs accumulate in the same file.
        try {
            Files.createDirectories(Paths.get("test-output"));
            Files.write(
                Paths.get(ORDER_FILE),
                record.toString().getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
            log.info("Order details saved to: {} (Order ID: {})", ORDER_FILE, orderId);
        } catch (IOException e) {
            log.error("Failed to save order details: {}", e.getMessage());
        }

        return orderId;
    }
}
