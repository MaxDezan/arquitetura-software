package domain;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class Product implements EntityInterface{
    private UUID uuid;
    private String sku;
    private String name;
    private float price;
    private Date datePrice;
    private ArrayList<Price> historicalPrice = new ArrayList<>();

    public Product() {
    }

    public Product(UUID uuid, String sku, String name, float price) {
        this.uuid = uuid;
        this.sku = sku;
        this.name = name;
        this.price = price;
    }

    public Product(String sku, String name, float price) {
        this.sku = sku;
        this.name = name;
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        if (this.price != 0f && this.datePrice != null){
            Price oldPrice = new Price(this.price, this.datePrice);
            historicalPrice.add(oldPrice);
        }
        this.price = price;
        this.datePrice = new Date();
    }

    public Date getDatePrice() {
        return datePrice;
    }

    public void setDatePrice(Date datePrice) {
        this.datePrice = datePrice;
    }

    public ArrayList<Price> getHistoricalPrice() {
        return historicalPrice;
    }

    public void setHistoricalPrice(ArrayList<Price> historicalPrice) {
        this.historicalPrice = historicalPrice;
    }

    @Override
    public String toString() {
        return "Product{" +
                "SKU='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", datePrice=" + datePrice +
                ", historicalPrice=" + historicalPrice +
                '}';
    }
}
