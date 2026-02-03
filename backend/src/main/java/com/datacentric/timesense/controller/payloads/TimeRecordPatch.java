
package com.datacentric.timesense.controller.payloads;

import java.util.List;

public class TimeRecordPatch {

    public String command;

    public Data data;

    public static class Data {
        public List<Long> ids;
        public String reason;
    }
}
