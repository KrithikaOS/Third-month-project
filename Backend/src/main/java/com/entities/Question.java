package com.entities;

import com.enums.Option;
import com.enums.QuestionStatus;
import com.googlecode.objectify.annotation.*;
import lombok.Data;

import java.util.Base64;
import java.util.Map;

@Entity
@Data
public class Question extends AbstractBaseEntity {

    @Index
    private String tag;

    private QuestionStatus status;

    @IgnoreSave
    private Option choosedOption;

    @Index
    private String description;

    @Serialize
    private Map<Option,String> option;

    private Option correctAns;

    @Index
    private int averageRating;

    private int noOfUsersRated;

    private int totalRating;


    @OnSave
    public void encryptDescription(){
        this.description= Base64.getEncoder().encodeToString(this.description.getBytes());
    }
    @OnLoad
    public void decryptDescription(){
        this.description= new String( Base64.getDecoder().decode(this.description));
    }


}
