package net.simcop.serverautostoponly;

import java.util.Calendar;

public class StopTime {

    public int Hour, Minute;

    public StopTime(int h, int m)
    {
        this.Hour = h;
        this.Minute = m;
    }

    public Boolean isNow()
    {
        int h, m, s;
        h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        m = Calendar.getInstance().get(Calendar.MINUTE);
        
        if(this.Hour == h && this.Minute == m)
            return true;
        return false;
    }

    public Boolean doWarn(int warn)
    {
        int timeuntil;
        int h, m, s, wh, wm, ws;
        h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        m = Calendar.getInstance().get(Calendar.MINUTE);
        s = Calendar.getInstance().get(Calendar.SECOND);

        /*
         * making sure my sanity is correct
         * We find out the current time, and subtract it from our target time, this tells us how many hours, how many minutes, how many seconds until then
         * a negative number here is an underflow and means that we need to take it from the higher units, e.g. negative seconds come from minutes, negative minutes from hours
         * the easiest way to do this is to scale them all to seconds and then add them all together, let the computer do it for us
         *
         * what this means is that any positive result is how many seconds we have until we get to the stop time
         * a negative result means we have PASSED the stop time and we should ignore a warning (this case can happen when you have many restart times)
         */
        ws = /*this.second*/-s; // how many seconds until we stop, our seconds are always zero
        wm = this.Minute - m; // how many minutes until we stop
        wh = this.Hour - h; // how many hours until we stop

        timeuntil = ws + (wm + wh * 60) * 60; // how many seconds

        if(timeuntil > 0 && timeuntil < warn) // if the time until is negative, we've passed it?
            return true;

        return false;
    }
}
