package domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product")
public class Product implements EntityInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", length = 36)
    private UUID uuid;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = false)
    @JoinColumn(name = "price_id")
    private Price price;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Price> historicalPrice = new ArrayList<>();

    public Product() {
    }

    public Product(String sku, String name, Price price) {
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

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price newPrice) {
        if (this.price != null) {
            this.price.setProduct(this);
            historicalPrice.add(this.price);
        }
        this.price = newPrice;
    }

    public void setPrice(Float value) {
        setPrice(new Price(value, new Date()));
    }

    public List<Price> getHistoricalPrice() {
        return historicalPrice;
    }

    public void setHistoricalPrice(List<Price> historicalPrice) {
        this.historicalPrice = historicalPrice;
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public String toString() {
        String currentPrice = price != null ? price.getPrice() + " @ " + price.getDate() : "none";
        String history = historicalPrice.stream()
                .map(p -> p.getPrice() + " @ " + p.getDate())
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
        return "Product { " +
                "uuid='" + uuid + "', " +
                "sku='" + sku + "', " +
                "name='" + name + "', " +
                "price=" + currentPrice + ", " +
                "history=" + history +
                " }";
    }
}