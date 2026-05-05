package domain;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "price")
public class Price implements EntityInterface {
    private UUID uuid;
    private Float price;
    private Date date;

    public Price() {
    }

    public Price(Float price, Date date) {
        this.price = price;
        this.date = date;
    }

    public Price(UUID uuid, Float price, Date date) {
        this.uuid = uuid;
        this.price = price;
        this.date = date;
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public String toString() {
        return "Price{" +
                "price=" + price +
                ", date=" + date +
                '}';
    }
}
