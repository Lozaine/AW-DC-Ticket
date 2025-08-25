package com.discordticketbot.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimestampUtil {

    public enum TimestampFormat {
        SHORT_TIME("t"),        // 16:20
        LONG_TIME("T"),         // 16:20:30
        SHORT_DATE("d"),        // 20/04/2021
        LONG_DATE("D"),         // 20 April 2021
        SHORT_DATE_TIME("f"),   // 20 April 2021 16:20
        LONG_DATE_TIME("F"),    // Tuesday, 20 April 2021 16:20
        RELATIVE_TIME("R");     // 2 months ago

        private final String format;

        TimestampFormat(String format) {
            this.format = format;
        }

        public String getFormat() {
            return format;
        }
    }

    /**
     * Get Discord timestamp for current time
     */
    public static String getCurrentDiscordTimestamp(TimestampFormat format) {
        long timestamp = System.currentTimeMillis() / 1000L;
        return "<t:" + timestamp + ":" + format.getFormat() + ">";
    }

    /**
     * Get Discord timestamp from milliseconds
     */
    public static String getDiscordTimestamp(long milliseconds, TimestampFormat format) {
        long timestamp = milliseconds / 1000L;
        return "<t:" + timestamp + ":" + format.getFormat() + ">";
    }

    /**
     * Get Discord timestamp from Instant
     */
    public static String getDiscordTimestamp(Instant instant, TimestampFormat format) {
        long timestamp = instant.getEpochSecond();
        return "<t:" + timestamp + ":" + format.getFormat() + ">";
    }

    /**
     * Get current timestamp for embeds (full date and time)
     */
    public static String getCurrentTimestampForEmbeds() {
        return getCurrentDiscordTimestamp(TimestampFormat.LONG_DATE_TIME);
    }

    /**
     * Get relative time for activity logs
     */
    public static String getCurrentRelativeTimestamp() {
        return getCurrentDiscordTimestamp(TimestampFormat.RELATIVE_TIME);
    }

    /**
     * Get readable timestamp for console logging (UTC+08 for Bintulu, Sarawak)
     */
    public static String getReadableTimestamp() {
        return LocalDateTime.now(ZoneOffset.of("+08:00"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " UTC+08";
    }
}