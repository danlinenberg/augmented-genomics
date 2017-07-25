package com.tau.application;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
/**
 * Created by dan on 06/09/16.
 */
@DynamoDBTable(tableName = "Patient")
public class Patient {
    private String name;
    private String password;
    private long id;

    //Used for our lambda interface, as opposed to DBHashkeys for Dynamodb
    public String Key;

    public Patient(String Key){
        this.Key = Key;
    }
    public Patient(){
    }


    @DynamoDBHashKey(attributeName = "id")
    public long getID(){
        return this.id;
    }
    public void setID(long id){
        this.id = id;
    }
    @DynamoDBAttribute(attributeName = "name")
    public String getName(){
        return this.name;
    }
    public void setName(String name){
        this.name = name;
    }
    @DynamoDBAttribute(attributeName = "password")
    public String getPassword(){
        return this.password;
    }
    public void setPassword(String password){
        this.password = password;
    }



}
