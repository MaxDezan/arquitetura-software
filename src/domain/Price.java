package domain;

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "price")
public class Price implements EntityInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "uuid", length = 36)
    private UUID uuid;

    @Column(name = "price")
    private Float price;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date")
    private Date date;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public Price() {
    }

    public Price(Float price, Date date) {
        this.price = price;
        this.date = date;
    }

    public Price(Product product, Float price, Date date) {
        this.product = product;
        this.price = price;
        this.date = date;
    }

    public Price(UUID uuid, Float price, Date date) {
        this.uuid = uuid;
        this.price = price;
        this.date = date;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public String toString() {
        return "Price { " +
                price + " @ " + date +
                ", product=" + (product != null ? product.getSku() : "none") +
                " }";
    }
}
