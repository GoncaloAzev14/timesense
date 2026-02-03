package com.datacentric.timesense.utils.i18n;

public final class MessagesCodes {

    // --------------------- Api Errors ---------------------
    public static final String INTERNAL_SERVER_ERROR = "API_INTERNAL_500_01";
    public static final String UNAUTHORIZED_USER = "API_INTERNAL_401_01";

    // --------------------- GENERAL ---------------------
    public static final String PERMISSIONS_SAVED = "API_PERM_200_01";
    public static final String PERMISSIONS_DENIED = "API_PERM_403_01";
    public static final String UNIQUE_NAME_VIOLATION = "BD_500_01";
    public static final String FILE_UPLOAD_FAILED = "API_FILE_500_01";

    // --------------------- VALIDATIONS ---------------------
    public static final String MALFORMED_REQUEST_BODY = "API_INTERNAL_400_01";
    public static final String VALIDATION_FAILED = "API_INTERNAL_400_02";
    public static final String DATA_INTEGRITY_VIOLATION = "API_INTERNAL_400_03";
    public static final String FIELD_MUST_BE_FILLED = "API_INTERNAL_400_04";
    public static final String FIELD_TOO_LONG = "API_INTERNAL_400_05";
    public static final String EMPTY_COMMAND_LIST = "API_INTERNAL_400_06";
    public static final String INVALID_COMMAND = "API_INTERNAL_400_07";
    public static final String MISSING_APPROVER = "API_INTERNAL_400_08";
    public static final String NO_FILES_UPLOADED = "API_INTERNAL_400_09";

    // --------------------- USER ---------------------
    public static final String USER_CREATED_OK = "API_USER_201_01";
    public static final String USER_DELETED_OK = "API_USER_202_01";
    public static final String USER_UPDATED_OK = "API_USER_202_02";
    public static final String USER_NOT_FOUND = "API_USER_404_01";
    public static final String USER_CREATED_ERROR = "API_USER_500_01";
    public static final String USERS_SYNCHRONIZED_OK = "API_USER_200_01";

    // --------------------- USER ROLE ---------------------
    public static final String ROLE_CREATED_OK = "API_ROLE_201_01";
    public static final String ROLE_DELETED_OK = "API_ROLE_202_01";
    public static final String ROLE_UPDATED_OK = "API_ROLE_202_02";
    public static final String ROLE_NOT_FOUND = "API_ROLE_404_01";

    // --------------------- USER_GROUP ---------------------
    public static final String USER_GROUP_CREATED_OK = "API_USRGRP_201_01";
    public static final String USER_GROUP_DELETED_OK = "API_USRGRP_202_01";
    public static final String USER_GROUP_UPDATED_OK = "API_USRGRP_202_02";
    public static final String USER_GROUP_NOT_FOUND = "API_USRGRP_404_01";

    // --------------------- CLIENT ---------------------
    public static final String CLIENT_CREATED_OK = "API_CLIENT_201_01";
    public static final String CLIENT_DELETED_OK = "API_CLIENT_202_01";
    public static final String CLIENT_UPDATED_OK = "API_CLIENT_202_02";
    public static final String CLIENT_NOT_FOUND = "API_CLIENT_404_01";
    public static final String CLIENT_CREATED_ERROR = "API_CLIENT_500_01";

    // --------------------- PROJECT TYPE ---------------------
    public static final String PROJECT_TYPE_CREATED_OK = "API_PROJECT_TYPE_201_01";
    public static final String PROJECT_TYPE_DELETED_OK = "API_PROJECT_TYPE_202_01";
    public static final String PROJECT_TYPE_UPDATED_OK = "API_PROJECT_TYPE_202_02";
    public static final String PROJECT_TYPE_NOT_FOUND = "API_PROJECT_TYPE_404_01";
    public static final String PROJECT_TYPE_CREATED_ERROR = "API_PROJECT_TYPE_500_01";
    public static final String TYPE_CONFLICT_PROJECT = "API_PROJECT_TYPE_409_01";

    // --------------------- PROJECT ---------------------
    public static final String PROJECT_CREATED_OK = "API_PROJECT_201_01";
    public static final String PROJECT_DELETED_OK = "API_PROJECT_202_01";
    public static final String PROJECT_UPDATED_OK = "API_PROJECT_202_02";
    public static final String PROJECT_CLOSED_OK = "API_PROJECT_202_03";
    public static final String PROJECT_NOT_FOUND = "API_PROJECT_404_01";
    public static final String PROJECT_CREATED_ERROR = "API_PROJECT_500_01";
    public static final String IMPORT_PROJ_CSV_ERROR = "API_PROJECT_IMPORT_400_01";
    public static final String PROJECT_CODE_CONFLICT = "API_PROJECT_409_01";

    // --------------------- PROJECT TASK ---------------------
    public static final String PROJECT_TASK_CREATED_OK =    "API_PROJECT_TASK_201_01";
    public static final String PROJECT_TASK_DELETED_OK =    "API_PROJECT_TASK_202_01";
    public static final String PROJECT_TASK_UPDATED_OK =    "API_PROJECT_TASK_202_02";
    public static final String PROJECT_TASK_NOT_FOUND =     "API_PROJECT_TASK_404_01";
    public static final String PROJECT_TASK_CREATED_ERROR = "API_PROJECT_TASK_500_01";

    // --------------------- PROJECT_ASSIGNMENT ---------------------
    public static final String PROJECT_ASSIGNMENT_CREATED_OK = "API_PROJECT_ASSIGNMENT_201_01";
    public static final String PROJECT_ASSIGNMENT_DELETED_OK = "API_PROJECT_ASSIGNMENT_202_01";
    public static final String PROJECT_ASSIGNMENT_UPDATED_OK = "API_PROJECT_ASSIGNMENT_202_02";
    public static final String PROJECT_ASSIGNMENT_NOT_FOUND = "API_PROJECT_ASSIGNMENT_404_01";
    public static final String PROJECT_ASSIGNMENT_CREATED_ERROR = "API_PROJECT_ASSIGNMENT_500_01";

    // --------------------- TIME_RECORD ---------------------
    public static final String TIME_RECORD_CREATED_OK = "API_TIME_RECORD_201_01";
    public static final String TIME_RECORD_DELETED_OK = "API_TIME_RECORD_202_01";
    public static final String TIME_RECORD_UPDATED_OK = "API_TIME_RECORD_202_02";
    public static final String TIME_RECORD_LIST_OK = "API_TIME_RECORD_202_03";
    public static final String TIME_RECORD_NOT_FOUND = "API_TIME_RECORD_404_01";
    public static final String TIME_RECORD_CREATED_ERROR = "API_TIME_RECORD_500_01";
    public static final String TIME_RECORD_LIST_ERROR = "API_TIME_RECORD_500_02";
    public static final String TIME_RECORD_INVALID_HOURS = "API_TIME_RECORD_400_01";
    public static final String TIME_RECORD_PROJECT_ACCESS_DENIED = "API_TIME_RECORD_403_01";
    public static final String TIME_RECORD_DUPLICATE_CONSTRAINT = "API_TIME_RECORD_409_01";

    // --------------------- ABSENCE ---------------------
    public static final String ABSENCE_CREATED_OK = "API_ABSENCE_201_01";
    public static final String ABSENCE_DELETED_OK = "API_ABSENCE_202_01";
    public static final String ABSENCE_UPDATED_OK = "API_ABSENCE_202_02";
    public static final String ABSENCE_BY_DATE_MAP_OK = "API_ABSENCE_200_01";
    public static final String ABSENCE_ATTACHMENTS_UPLOADED_OK = "API_ABSENCE_200_02";
    public static final String ABSENCE_NOT_FOUND = "API_ABSENCE_404_01";
    public static final String ABSENCE_CREATED_ERROR = "API_ABSENCE_500_01";
    public static final String INSUFFICIENT_VACS_DAYS = "API_INVALID_VACS_400_01";
    public static final String IMPORT_ABSENCE_CSV_ERROR = "API_ABSENCE_IMPORT_400_01";
    
    // --------------------- ABSENCE ATTACHMENT ---------------------
    public static final String ATTACHMENT_NOT_FOUND = "API_FILE_404_01";
    public static final String ATTACHMENT_NOT_FROM_ABSENCE = "API_FILE_400_01";
    public static final String ABSENCE_ATTACHMENT_DELETED_OK = "API_ABSENCE_ATTACHMENT_202_01";
    public static final String ABSENCE_ATTACHMENTS_FOUND_OK = "API_ABSENCE_ATTACHMENT_200_01";

    // --------------------- ABSENCE_TYPE ---------------------
    public static final String ABSENCE_TYPE_CREATED_OK = "API_ABSENCE_TYPE_201_01";
    public static final String ABSENCE_TYPE_DELETED_OK = "API_ABSENCE_TYPE_202_01";
    public static final String ABSENCE_TYPE_UPDATED_OK = "API_ABSENCE_TYPE_202_02";
    public static final String ABSENCE_TYPE_NOT_FOUND = "API_ABSENCE_TYPE_404_01";
    public static final String ABSENCE_TYPE_CREATED_ERROR = "API_ABSENCE_TYPE_500_01";

    // --------------------- ABSENCE SUB TYPE ---------------------
    public static final String ABSENCE_SUB_TYPE_CREATED_OK = "API_ABSENCE_SUB_TYPE_201_01";
    public static final String ABSENCE_SUB_TYPE_DELETED_OK = "API_ABSENCE_SUB_TYPE_202_01";
    public static final String ABSENCE_SUB_TYPE_UPDATED_OK = "API_ABSENCE_SUB_TYPE_202_02";
    public static final String ABSENCE_SUB_TYPE_NOT_FOUND = "API_ABSENCE_SUB_TYPE_404_01";
    public static final String ABSENCE_SUB_TYPE_CREATED_ERROR = "API_ABSENCE_SUB_TYPE_500_01";

    // --------------------- HOLIDAY ---------------------
    public static final String HOLIDAY_CREATED_OK = "API_HOLIDAY_201_01";
    public static final String HOLIDAY_DELETED_OK = "API_HOLIDAY_202_01";
    public static final String HOLIDAY_UPDATED_OK = "API_HOLIDAY_202_02";
    public static final String HOLIDAY_NOT_FOUND = "API_HOLIDAY_404_01";
    public static final String HOLIDAY_CREATED_ERROR = "API_HOLIDAY_500_01";
    public static final String IMPORT_HOLIDAY_CSV_ERROR = "API_HOLIDAY_IMPORT_400_01";
    public static final String RECALCULATE_TIMEOFF_ERROR = "API_RECALC_TIMEOFF_400_01";

    // --------------------- JOB_TITLE ---------------------
    public static final String JOB_TITLE_CREATED_OK = "API_JOB_TITLE_201_01";
    public static final String JOB_TITLE_DELETED_OK = "API_JOB_TITLE_202_01";
    public static final String JOB_TITLE_UPDATED_OK = "API_JOB_TITLE_202_02";
    public static final String JOB_TITLE_NOT_FOUND = "API_JOB_TITLE_404_01";
    public static final String JOB_TITLE_CONFLICT_USER = "API_JOB_TITLE_409_01";

    // --------------------- STATUS ---------------------
    public static final String STATUS_CREATED_OK = "API_STATUS_201_01";
    public static final String STATUS_DELETED_OK = "API_STATUS_202_01";
    public static final String STATUS_UPDATED_OK = "API_STATUS_202_02";
    public static final String STATUS_NOT_FOUND = "API_STATUS_404_01";
    public static final String STATUS_CREATED_ERROR = "API_STATUS_500_01";

    // --------------------- SYSTEM_SETTING ---------------------
    public static final String SYSTEM_SETTING_CREATED_OK = "API_SYSTEM_SETTING_201_01";
    public static final String SYSTEM_SETTING_DELETED_OK = "API_SYSTEM_SETTING_202_01";
    public static final String SYSTEM_SETTING_UPDATED_OK = "API_SYSTEM_SETTING_202_02";
    public static final String CLOSED_BUSINESS_YEAR_OK = "API_SYSTEM_SETTING_202_03";
    public static final String SYSTEM_SETTING_NOT_FOUND = "API_SYSTEM_SETTING_404_01";
    public static final String SYSTEM_SETTING_CREATED_ERROR = "API_SYSTEM_SETTING_500_01";
    public static final String SYSTEM_SETTING_NOT_EDITABLE = "API_SYSTEM_SETTING_403_01";

    private MessagesCodes() {
    }
}
