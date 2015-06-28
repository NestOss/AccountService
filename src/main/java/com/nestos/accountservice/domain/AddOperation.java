package com.nestos.accountservice.domain;

import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import static org.apache.commons.lang3.Validate.inclusiveBetween;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Add value operation. Immutable.
 *
 * @author Roman Osipov
 */
public final class AddOperation implements Serializable {

    //-------------------Logger---------------------------------------------------
    //-------------------Constants------------------------------------------------

    public static final String ID_IAE_MESSAGE = "id can't be negative.";

    //-------------------Fields---------------------------------------------------
    // Balance identifier.  
    private final int id;

    // Amount to add. 
    private final long value;

    //-------------------Constructors---------------------------------------------
    public AddOperation(int id, long value) {
        inclusiveBetween(0, Integer.MAX_VALUE, id, ID_IAE_MESSAGE);
        this.id = id;
        this.value = value;
    }

    //-------------------Getters and setters--------------------------------------
    public int getId() {
        return id;
    }

    public long getValue() {
        return value;
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
    
   /**
    * Encode instance to byte array.
    * @return byte array instance representation.
    */ 
   public byte[] toByteArray() {
       return SerializationUtils.serialize(this);
    }
    
   /**
    * Decode instance from byte array.
    * @param data source byte array.
    * @return created AddOperation instance. 
    */
    public static AddOperation valueOf(byte[] data) {
       return (AddOperation) SerializationUtils.deserialize(data); 
    } 
}
