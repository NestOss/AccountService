package com.nestos.accountservice.domain;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import static org.apache.commons.lang3.Validate.inclusiveBetween;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Kafka partition.
 *
 * @author Roman Osipov.
 */
@Entity
@Table(name="kafkapartition")
public class Partition implements Serializable {
    //-------------------Logger---------------------------------------------------
    //-------------------Constants------------------------------------------------

    private static final long serialVersionUID = 1L;
    public static final String ID_IAE_MESSAGE = "id can't be negative.";
    public static final String OFFSET_IAE_MESSAGE = "id can't be negative.";

    //-------------------Fields---------------------------------------------------
    // Partition number.
    @Id
    private Integer id;

    // Last successeful read offset in partition.
    private Long offset = 0L;

    //-------------------Constructors---------------------------------------------
    public Partition() {
    }

    //-------------------Getters and setters--------------------------------------
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        inclusiveBetween(0, Integer.MAX_VALUE, id, ID_IAE_MESSAGE);
        this.id = id;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        inclusiveBetween(0, Long.MAX_VALUE, id, OFFSET_IAE_MESSAGE);
        this.offset = offset;
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
