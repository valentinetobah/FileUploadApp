/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package za.co.xone.controller.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author valentine
 */
public class XoneVerification {

    // Check if the file is of right format as specified
    public static String fileFormatValidation(String url) {
        File file = new File(url);
        String fileType = "";

        String message = null;

        try {
            fileType = Files.probeContentType(Paths.get(url));

            //Converting the file size to Megabytes
            double bytes = file.length();
            double MB = (bytes / (1024 * 1024));

            //Formatting the file size to 5 decimal places
            DecimalFormat df = new DecimalFormat("#.#####");
            String a = df.format(MB);
            int size = Integer.parseInt(a);

            //checking is if size is greater than 10MB
            if (size > 10) {
                message = "File size larger than 10MB";
            } //Checking if file is empty
            else if (file.getName().isEmpty()) {
                message = "File is Empty";
            } //Checking if file is a text file
            else if (!"text/plain".equalsIgnoreCase(fileType)) {
                message = "Not a text file";
            }

        } catch (IOException | NumberFormatException e) {
            Logger.getLogger(XoneVerification.class.getName()).log(Level.SEVERE, null, e);
        }
        return message;
    }

    //Formatting the date String to a SimpleDateFormat
    public static String dateFormatter(String actionDate) {
        Date actionedDate = null;
        String date = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("DD/MM/YYYY");
            actionedDate = sdf.parse(actionDate);
            date = sdf.format(actionedDate);

        } catch (ParseException ex) {
            System.out.println(ex);
        }
        return date;
    }

    //Checking if the number format is valid 
    public static boolean numberValidation(String number) {

        String phoneNumberRegex = "^(27)(\\d{9})$";
        Pattern pattern = Pattern.compile(phoneNumberRegex);

        Matcher matcher = pattern.matcher(number);

        //Checing if the number length is valid 
        //and the number matches the required format
        if (!(number.length() >= 12) && matcher.matches()) {
            return true;
        } else {
            return false;
        }

    }

}
