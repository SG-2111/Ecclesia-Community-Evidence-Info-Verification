package utils;


public class Validation {


    public static boolean validateEmail(String email){
        return email.contains("@") && email.contains(".");
    }


    public static boolean validateName(String name){
        return name != null && !name.trim().isEmpty();
    }


    public static boolean validateInput(String input){
        return input != null && !input.trim().isEmpty();
    }

}