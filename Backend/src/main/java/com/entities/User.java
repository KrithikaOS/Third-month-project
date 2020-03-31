package com.entities;

import com.enums.AccountType;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Serialize;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Entity
@Data
public class User extends AbstractBaseEntity {

    private String firstName;
    private String lastName;
    @Index
    private String email;
    private String password;
    private String company;
    private AccountType accountType;
    @Serialize
    private Map<String,Integer> userRatedQuestion;


}
