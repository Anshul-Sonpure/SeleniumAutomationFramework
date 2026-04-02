package com.automation.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("website")
    private String website;

    @JsonProperty("address")
    private Address address;

    @JsonProperty("company")
    private Company company;

    public User() {}

    public int     getId()                   { return id; }
    public void    setId(int id)             { this.id = id; }

    public String  getName()                 { return name; }
    public void    setName(String name)      { this.name = name; }

    public String  getUsername()             { return username; }
    public void    setUsername(String u)     { this.username = u; }

    public String  getEmail()                { return email; }
    public void    setEmail(String email)    { this.email = email; }

    public String  getPhone()                { return phone; }
    public void    setPhone(String phone)    { this.phone = phone; }

    public String  getWebsite()              { return website; }
    public void    setWebsite(String site)   { this.website = site; }

    public Address getAddress()              { return address; }
    public void    setAddress(Address a)     { this.address = a; }

    public Company getCompany()              { return company; }
    public void    setCompany(Company c)     { this.company = c; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {

        @JsonProperty("street")
        private String street;

        @JsonProperty("city")
        private String city;

        @JsonProperty("zipcode")
        private String zipcode;

        public Address() {}

        public String getStreet()              { return street; }
        public void   setStreet(String street) { this.street = street; }

        public String getCity()                { return city; }
        public void   setCity(String city)     { this.city = city; }

        public String getZipcode()             { return zipcode; }
        public void   setZipcode(String zip)   { this.zipcode = zip; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Company {

        @JsonProperty("name")
        private String name;

        public Company() {}

        public String getName()             { return name; }
        public void   setName(String name)  { this.name = name; }
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
