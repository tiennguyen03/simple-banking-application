package com.bootcamp.banking.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "customers")
public class Customer {

    @Id
    private String id;

    private String name;

    // Empty Constructor
    public Customer(){
    }

    // Constructor
    public Customer(String id, String name) {

        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        
    }

    public String getName() {
        return name;

    }

    public void setName(String name){
        this.name = name;
        
    }





    
}
