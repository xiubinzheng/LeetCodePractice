package net.shutingg.oa;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class NinetiethPercentile {
    public static void main(String args[] ) throws Exception {
        Scanner scanner = new Scanner(System.in);
        boolean sameLine = false;
        ZonedDateTime min = null;
        TimeCache timeCache = new TimeCache();

        while (scanner.hasNext()) {
            if (!sameLine) {
                long timestamp = scanner.nextLong();
                ZonedDateTime zonedDateTime = convertTimeStampToZonedDateTime(timestamp);
                min = zonedDateTime.truncatedTo(ChronoUnit.MINUTES);
            } else {
                double duration = scanner.nextDouble();

                if (timeCache.contains(min)) {
                    timeCache.insertTime(min, duration);
                } else {
                    timeCache.insertTime(min, duration);

                    ZonedDateTime twoMinAgo = min.minusMinutes(2);
                    if (timeCache.contains(twoMinAgo)) {
                        String output = timeCache.flush(twoMinAgo);
                        //required output
                        System.out.println(output);
                    }
                }
            }
            sameLine = !sameLine;
        }

        List<String> rest = timeCache.flushAll();
        for (String s : rest) {
            System.out.println(s);
        }
    }

    private static ZonedDateTime convertTimeStampToZonedDateTime(long timestamp) {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.of("GMT-0"));
    }

    /**
     * Partially cache response time so that 90th percentile can be estimated like taking a sample.
     */
    static class TimeCache {
        final int CACHE_SIZE = 1000;
        Random rand = new Random();
        Map<ZonedDateTime, Double[]> cache = new HashMap<>();
        Map<ZonedDateTime, Integer> cachePlace = new HashMap<>();

        /**
         * Insert response time to the relevant group
         *
         * @param time truncated at minute so that it can be grouped
         * @param t response time
         */
        void insertTime(ZonedDateTime time, double t) {
            if (t < 0 || t > 150) {
                return;
            }

            Double[] slots = cache.get(time);
            if (slots == null) {
                slots = new Double[CACHE_SIZE];
                cache.put(time, slots);
                cachePlace.put(time, 0);
            }

            int pl = cachePlace.get(time);

            if (pl < CACHE_SIZE) {
                slots[pl] = t;
                pl++;
                cachePlace.put(time, pl);
            } else {
                if (rand.nextBoolean()) {
                    int p = rand.nextInt(CACHE_SIZE);
                    slots[p] = t;
                }
            }
        }

        /**
         * Get the 90th percentile by sorting the sample
         *
         * @param time truncated at minute
         * @return response time at 90th percentile of the sample
         */
        double getNinetiethTime(ZonedDateTime time) {
            Double[] slots = cache.get(time);
            int pl = cachePlace.get(time);

            Arrays.sort(slots, (a, b) -> {
                if (a == null && b == null) {
                    return 0;
                }
                if (a == null) {
                    return 1;
                }
                if (b == null) {
                    return -1;
                }
                return b.compareTo(a);
            });
            int p = (int) Math.floor( pl * 0.1);
            return slots[p];
        }

        /**
         * Whether time has been stored
         *
         * @param time truncated at minute
         * @return true if time has been stored
         */
        boolean contains(ZonedDateTime time) {
            return cache.containsKey(time);
        }

        /**
         * Get 90th percentile of time, clear related data and return a formatted time and duration string
         *
         * @param time truncated at minute
         * @return format like "2000-07-04T00:00:00Z 69.5"
         */
        String flush(ZonedDateTime time) {
            double ninetiethTime = getNinetiethTime(time);
            String sTime = time.format(DateTimeFormatter.ISO_INSTANT);

            //required output
            String output = sTime + " "+ ninetiethTime;

            cache.remove(time);
            cachePlace.remove(time);

            return output;
        }

        /**
         * Generate a list of the 90th percentile response time for every minute stored and clear the data
         *
         * @return list of string in format like "2000-07-04T00:00:00Z 69.5"
         */
        List<String> flushAll() {
            List<ZonedDateTime> list = new ArrayList<>(cache.keySet());
            Collections.sort(list);
            List<String> res = new ArrayList<>();
            for (ZonedDateTime key : list) {
                String output = flush(key);
                res.add(output);
            }
            return res;
        }
    }
}