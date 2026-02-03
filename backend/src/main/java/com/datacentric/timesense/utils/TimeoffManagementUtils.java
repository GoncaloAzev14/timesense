package com.datacentric.timesense.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.datacentric.exceptions.DataCentricException;
import com.datacentric.timesense.model.Absence;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.AbsenceRepository;
import com.datacentric.timesense.repository.UserRepository;

/*
 * Utility class to guarantee the useres remaining vacation days maintain consistent
 */
@Service
public class TimeoffManagementUtils {

    private static final Logger log = LoggerFactory.getLogger(TimeoffManagementUtils.class);

    private static final double ZERO = 0.0;
    private static final double BUSINESS_DAY_HOURS = 8.0;
    private static final int DECIMAL_PLACES = 2;

    private UserRepository userRepository;
    private AbsenceRepository absenceRepository;

    @Autowired
    public TimeoffManagementUtils(UserRepository userRepository,
            AbsenceRepository absenceRepository) {
        this.userRepository = userRepository;
        this.absenceRepository = absenceRepository;
    }

    /*
     * Utility method to recalculate users remaining vacations days every time a new
     *  timeoff configuration is set
     *      Receives a list of the new holidays dates and a list of the holidays dates removed
     *          and based on that calculates the new work days balance to update each affected
     *          absence and user
     */
    public void recalculateUsersAbsences(List<LocalDate> newHolidays,
            List<LocalDate> removedDates) throws DataCentricException {
        try {
            if (newHolidays != null && !newHolidays.isEmpty()) {
                handleNewHolidays(newHolidays);
            }
            if (removedDates != null && !removedDates.isEmpty()) {
                handleRemovedHolidays(removedDates);
            }

        } catch (Exception e) {
            throw new DataCentricException(
                "Error Recalculating the Users Absences and Remaining Vacations Days", e);
        }
    }

    private void handleNewHolidays(List<LocalDate> newHolidays) {
        // handle new vacation days
        for (LocalDate newDate: newHolidays) {
            Timestamp refDate = Timestamp.valueOf(newDate.atStartOfDay());
            List<Absence> influencedAbsences = absenceRepository
                    .findAllAbsencesByDate(refDate);
            List<User> usersToUpdate = new ArrayList<>();
            List<Absence> absensecesToUpdate = new ArrayList<>();

            for (Absence abs : influencedAbsences) {
                String currentYear = String.valueOf(Year.now().getValue());
                String prevYear = String.valueOf(Year.now().getValue() - 1);
                double newWorkDays = abs.getWorkDays() - 1.0;
                log.info("Updating absence {} because it was impacted " +
                        "by the new holidays configuration", abs.getName());
                abs.setWorkDays(newWorkDays);
                abs.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                absensecesToUpdate.add(abs);

                User user = userRepository.findById(abs.getUser().getId()).get();
                if (abs.getBusinessYear().equals(currentYear)) {
                    log.info("Updating user current year remaining vacation days because it was" +
                        " impacted by the new holidays configuration", abs.getName());
                    double newRemainingDays = user.getCurrentYearVacationDays() + 1.0;
                    user.setCurrentYearVacationDays(newRemainingDays);
                    user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                    usersToUpdate.add(user);
                } else if (abs.getBusinessYear().equals(prevYear)) {
                    log.info("Updating user previous year remaining vacation days because it " +
                        "was impacted by the new holidays configuration", abs.getName());
                    double newRemainingDays = user.getCurrentYearVacationDays() + 1.0;
                    user.setCurrentYearVacationDays(newRemainingDays);
                    user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                    usersToUpdate.add(user);
                }
            }
            absenceRepository.saveAll(absensecesToUpdate);
            userRepository.saveAll(usersToUpdate);
        }
    }

    private void handleRemovedHolidays(List<LocalDate> removedHolidays) {
        // handle removed vacation days
        for (LocalDate newDate: removedHolidays) {
            Timestamp refDate = Timestamp.valueOf(newDate.atStartOfDay());
            List<Absence> influencedAbsences = absenceRepository
                    .findAllAbsencesByDate(refDate);
            List<User> usersToUpdate = new ArrayList<>();
            List<Absence> absensecesToUpdate = new ArrayList<>();

            for (Absence abs : influencedAbsences) {
                String currentYear = String.valueOf(Year.now().getValue());
                String prevYear = String.valueOf(Year.now().getValue() - 1);
                double newWorkDays = abs.getWorkDays() + 1.0;
                abs.setWorkDays(newWorkDays);
                abs.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                absensecesToUpdate.add(abs);

                User user = userRepository.findById(abs.getUser().getId()).get();
                if (abs.getBusinessYear().equals(currentYear)) {
                    double newRemainingDays = user.getCurrentYearVacationDays() - 1.0;
                    user.setCurrentYearVacationDays(newRemainingDays);
                    user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                    usersToUpdate.add(user);
                } else if (abs.getBusinessYear().equals(prevYear)) {
                    double newRemainingDays = user.getCurrentYearVacationDays() - 1.0;
                    user.setCurrentYearVacationDays(newRemainingDays);
                    user.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
                    usersToUpdate.add(user);
                }
            }
            absenceRepository.saveAll(absensecesToUpdate);
            userRepository.saveAll(usersToUpdate);
        }
    }

    /**
    * Converts the given number of hours into a fraction of a business day.
    * For example:
    *  - 8 hours -> 1.0
    *  - 4 hours -> 0.5
    *  - 0 hours -> 0.0
    *
    * @param hours the number of hours (0 to 8)
    * @return the equivalent value in business days
    * @throws IllegalArgumentException if hours is negative or greater than 8
    */
    public static double hoursToBusinessDays(double hours) {
        if (hours < ZERO || hours > BUSINESS_DAY_HOURS) {
            throw new IllegalArgumentException("Hours must be between 0 and 8 inclusive.");
        }
        double result = hours / BUSINESS_DAY_HOURS;
        // Round to 2 decimal places
        return BigDecimal.valueOf(result)
                .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
