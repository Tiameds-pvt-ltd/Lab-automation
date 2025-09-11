package tiameds.com.tiameds.utils;

public class SnowflakeIdGenerator {

    // Bits allocation
    private final long twepoch = 1672531200000L; // custom epoch (Jan 1, 2023)
    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long sequenceBits = 12L;

    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);       // 31
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits); // 31
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);        // 4095

    private final long workerIdShift = sequenceBits;                 // 12
    private final long datacenterIdShift = sequenceBits + workerIdBits; // 17
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits; // 22

    private long workerId;
    private long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("workerId must be between 0 and " + maxWorkerId);
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId must be between 0 and " + maxDatacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + (lastTimestamp - timestamp) + "ms");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // Sequence overflow, wait until next millisecond
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}



