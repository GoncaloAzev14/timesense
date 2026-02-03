package com.datacentric.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.datacentric.timesense.model.Holiday;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class FileParseUtils {
    private static final String CSV_DELIMITER = ",";
    
    private FileParseUtils() {
    }
    
    public static List<Holiday> parseHolidaysFromCsv(InputStream inputStream) throws IOException, 
            CsvValidationException {

        List<Holiday> holidays = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            boolean isFirstLine = true;

            while ((line = csvReader.readNext()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                try {
                    LocalDate date = LocalDate.parse(line[0].trim());
                    String name = line[1].trim();
                    
                    Holiday holiday = new Holiday();
                    holiday.setHolidayDate(date);
                    holiday.setName(name);

                    holidays.add(holiday);
                } catch (DateTimeParseException | ArrayIndexOutOfBoundsException ex) {
                    throw new IllegalArgumentException("Invalid CSV format: " + 
                        Arrays.toString(line), ex);
                }
            }
        }

        return holidays;

    }

    // CHECKSTYLE.OFF: MultipleStringLiterals
    public static String escapeCsv(String input) {
        if (input == null) return "";

        // Replace newlines with a single space
        String newValue = input.replaceAll("[\\r\\n]+", " ");

        if (newValue.contains(CSV_DELIMITER) || newValue.contains("\"")) {
            newValue = newValue.replace("\"", "\"\"");
            return "\"" + newValue + "\"";
        }
        return newValue;
    }
}
