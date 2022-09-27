package com.example.project_ds_2021;
import java.io.Serializable;
import java.util.Objects;

public class Topic implements Serializable{//Object που κρατά το όνομα ενός topic και το αν είναι κανάλι ή hashtag
    static final long serialVersionUID = 44L;
    private String name;
    private String type;

    public Topic(String name, String type){

        this.name = name;
        this.type = type;
    }

    public String getName(){
        return name;
    }

    public String getType(){
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals (Object topic) {
        boolean result;

        if (!((Topic) topic).getName().equals(this.getName())) { //οι συγκρίσεις μεταξύ topics γίνονται με βάση τ'όνομά τους
            result = false;
        } else {
            result = true;
        }
        return result;
    }
}
