package com.nestos.accountservice.domain;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import static org.apache.commons.lang3.Validate.*;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Balance account.
 *
 * @author Roman Osipov
 */
@Entity
public class Account implements Serializable {

    //-------------------Logger---------------------------------------------------
    //-------------------Constants------------------------------------------------
    private static final long serialVersionUID = 1L;
    public static final String ID_IAE_MESSAGE = "id can't be negative.";
    public static final String AMOUNT_NPE_MESSAGE = "amount can't be null.";
    
    //-------------------Fields---------------------------------------------------
    // Balance identifier.
    @Id
    private Integer id;

    // Total amount.
    private Long amount = 0L;

    //-------------------Constructors---------------------------------------------
    
    public Account() {  
    }
    
    //-------------------Getters and setters--------------------------------------
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        inclusiveBetween(0, Integer.MAX_VALUE, id, ID_IAE_MESSAGE);
        this.id = id;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        notNull(amount, AMOUNT_NPE_MESSAGE);
        this.amount = amount;
    }

    //-------------------Methods--------------------------------------------------
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object that) {
        return EqualsBuilder.reflectionEquals(this, that);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
    
}
