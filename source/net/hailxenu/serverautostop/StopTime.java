package net.hailxenu.serverautostop;

import java.util.Calendar;

public class StopTime {

    public int Hour, Minute, Second;

    public StopTime(int h, int m, int s)
    {
        this.Hour = h;
        this.Minute = m;
        this.Second = s;
    }

    public Boolean isNow()
    {
        int h, m, s;
        h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        m = Calendar.getInstance().get(Calendar.MINUTE);
        s = Calendar.getInstance().get(Calendar.SECOND);
        
        if(this.Hour == h && this.Minute == m && this.Second == s)
            return true;
        return false;
    }

    public Boolean doWarn(int warnSec)
    {
        int h, m, s, wh, wm, ws;
        h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        m = Calendar.getInstance().get(Calendar.MINUTE);
        s = Calendar.getInstance().get(Calendar.SECOND);

        ws = warnSec % 60 + s;
        wm = (ws/60) + m;
        wh = (wm/60) + h;

        if(ws>60)
            wm++;
        if(wm>60)
            wh++;
        if(wh>23)
            wh %= 24;

        if(this.Hour == wh && this.Minute == wm && this.Second == ws)
            return true;

        return false;
    }
}
