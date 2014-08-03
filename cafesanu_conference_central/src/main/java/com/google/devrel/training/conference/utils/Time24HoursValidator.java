package com.google.devrel.training.conference.utils;

import java.util.regex.Pattern;
 
public class Time24HoursValidator{

      private static final String TIME24HOURS_PATTERN = 
                        "([0-1][0-9]|2[0-3]):[0-5][0-9]";
      private static Pattern pattern = Pattern.compile(TIME24HOURS_PATTERN);
 
 
      public Time24HoursValidator(){
          pattern = Pattern.compile(TIME24HOURS_PATTERN);
      }
 
      /**
       * Validate time in 24 hours format with regular expression
       * @param time time address for validation
       * @return true valid time format, false invalid time format
       */
      public static boolean validate(final String time){
          return pattern.matcher(time).matches();
 
      }
}