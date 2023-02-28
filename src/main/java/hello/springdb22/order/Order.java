package hello.springdb22.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "orders")
@Getter
public class Order {
    @Id
    @GeneratedValue
    private Long id;

    private String username;
    private String payStatus;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }
}
