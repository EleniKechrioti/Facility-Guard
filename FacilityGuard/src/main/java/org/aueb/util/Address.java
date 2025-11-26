package org.aueb.util;

import jakarta.persistence.*;

/**
 * The mailing address.
 */
@Embeddable
public class Address {

    @Column(name="street", length=50)
    private String street;

    @Column(name="number", length = 10)
    private String number;

    @Column(name="city", length = 50)
    private String city;

    @Column(name="zipcode", length=50)
    private String zipcode;

    @Column(name="country", length=50)
    private String country = "Ελλάδα";

    /**
     * Default Constructor
     */
    public Address() { }

    /**
     * Helper Constructor that copies another address
     * @param address the other address
     */
    public Address(Address address) {
        this.street = address.getStreet();
        this.number = address.getNumber();
        this.city = address.getCity();
        this.zipcode = address.getZipCode();
        this.country = address.getCountry();
    }

    public Address(String street, String number, String city, String zipcode, String country){
        this.street = street;
        this.number = number;
        this.city = city;
        this.zipcode = zipcode;
        this.country = country;
    }
    /**
     * Sets the street
     * @param street the street
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * Return the street
     */
    public String getStreet() {
        return street;
    }

    /**
     * Sets the number
     * @param number the number
     */
    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * Returns the number
     */
    public String getNumber() {
        return number;
    }

    /**
     * Sets the city
     */
    public void setCity(String city) {
        this.city = city;
    }


    /**
     * Returns the city
     */
    public String getCity() {
        return city;
    }

    /**
     * Sets the zip code
     */
    public void setZipCode(String zipcode) {
        this.zipcode = zipcode;
    }

    /**
     * Returns the zipcode
     */
    public String getZipCode() {
        return zipcode;
    }

    /**
     * Sets the country
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Returns the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * Equality depends on all fields of the address
     * @param other the other object
     * @return  {@code true} if the objects are equal
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (!(other instanceof Address)) {
            return false;
        }

        Address theAddress = (Address) other;
        if (!(street == null ? theAddress.street
                == null : street.equals(theAddress.street))) {
            return false;
        }
        if (!(number == null ? theAddress.number
                == null : number.equals(theAddress.number))) {
            return false;
        }
        if (!(city == null ? theAddress.city
                == null : city.equals(theAddress.city))) {
            return false;
        }
        if (!(zipcode == null ? theAddress.zipcode
                == null : zipcode.equals(theAddress.zipcode))) {
            return false;
        }
        if (!(country == null ? theAddress.country
                == null : country.equals(theAddress.country))) {
            return false;
        }
        return true;
    }


    @Override
    public int hashCode() {
        if (street == null && number == null && city == null
                && zipcode == null && country == null) {
            return 0;
        }

        int result = 0;
        result = street == null ? result : 13 * result + street.hashCode();
        result = number == null ? result : 13 * result + number.hashCode();
        result = city == null ? result : 13 * result + city.hashCode();
        result = zipcode == null ? result : 13 * result + zipcode.hashCode();
        result = country == null ? result : 13 * result + country.hashCode();
        return result;
    }

}