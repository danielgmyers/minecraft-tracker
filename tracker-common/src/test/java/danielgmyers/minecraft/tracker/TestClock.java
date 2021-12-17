package danielgmyers.minecraft.tracker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class TestClock extends Clock {

    private Instant curTime;

    public TestClock(Instant startTime) {
        this.curTime = startTime;
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return curTime;
    }

    @Override
    public long millis() {
        return curTime.toEpochMilli();
    }

    public void forward(Duration amount) {
        curTime = curTime.plus(amount);
    }
}
