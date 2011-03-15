package autorestart;

import java.lang.Process;
import java.io.*;

public class Main {

    
    public static void main(String[] args){
        try{
        Thread.sleep(3500);

        String Path = "";

        for(String s : args)
        {
            Path += s + " ";
        }
        
        Process p;
        if(System.getProperty("os.name").toLowerCase().contains("win"))
        {
            BufferedWriter bw;
            p = Runtime.getRuntime().exec("cmd.exe /c start cmd.exe /k " + Path);
            bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            bw.write(Path + "\r\n");
            bw.flush();
        }else{
            Runtime.getRuntime().exec(Path);
        }

        }catch(Exception e)
        {
            try{
                FileWriter fw = new FileWriter("restart_log.txt");
                fw.write(e.toString().replace("\n", "\r\n"));
                fw.flush();
                fw.close();
            }catch(Exception ex){ }
        }
    }

}
