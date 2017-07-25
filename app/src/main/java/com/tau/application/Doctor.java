package com.tau.application;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

/**
 * Created by dan on 04/10/16.
 */
@DynamoDBTable(tableName = "Doctor")

public class Doctor {
    private String name;
    private String password;
    public long id;
    private int access;
    public String Key;

    public Doctor(String Key, long id)
    {
        this.Key = Key;
        this.id = id;
    }
    public Doctor(){
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
    @DynamoDBAttribute(attributeName = "access")
    public int getAccessLevel(){
        return this.access;
    }
    public void setAccessLevel(int level){
        this.access = level;
    }
    @DynamoDBAttribute(attributeName = "password")
    public String getPassword(){
        return this.password;
    }
    public void setPassword(String password){
        this.password = password;
    }

}