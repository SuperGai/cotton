package com.agilecontrol.phone;
/*
 * revised according to ant's Exec
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.util.Tools;
/**
 * Executes a given command if the os platform is appropriate.
 *
 */
@Admin(mail="yfzhu@lifecycle.cn")
public class CommandExecuter{
    private static Logger logger= LoggerFactory.getLogger(CommandExecuter.class);
    private boolean isLogging= true;
    private String out; // output file name
    @SuppressWarnings("unused")
	private String command;
    private Process proc;
    private StreamPumper inputPumper,errorPumper;
    private PrintWriter fos=null;
    // for bg process that spawned here
    //key:String(CommandExecuter.command) value:CommandExecuter( only running one)
    @SuppressWarnings({ "unchecked", "rawtypes" })
	private static Hashtable<String, CommandExecuter> bgProcesses=new Hashtable();

    private static final int BUFFER_SIZE = 512;
    
    private PrintStream ps;
    public void setLogging(boolean log){
      isLogging=log;
    }
    /**
     * 是否写入system.out，主要面向MintConsole
     * @param 
     */
    public void setPrintStream(PrintStream ps){
    	this.ps=ps;
    }

    /**
     * @param out the output file name
     */
    public CommandExecuter(String out){
        this.out=out;
    }
    public static void stopAllBackgroundProcess(){
        Enumeration<String> enu= bgProcesses.keys();
        while( enu.hasMoreElements()){
           stopBackgroundProcess((String)enu.nextElement());
        }
    }
    /**
     * Return process by command if it's in the processlist
     */
    public static Process getBackgroundProecess(String preCommand){
        Process p=null;
        CommandExecuter ce=((CommandExecuter)bgProcesses.get(preCommand));
        if( ce!=null) p=ce.proc;
        return p;
    }
    /**
     * Stop previous command if it's still running, and close it's output file
     * @return process exitValue
     */
    public static int stopBackgroundProcess(String preCommand){
        int ret=0;
        synchronized(bgProcesses){
            CommandExecuter ce=(CommandExecuter)bgProcesses.get(preCommand);
            if( ce ==null) return ret;
            try{
                if( ce.proc !=null) ce.proc.destroy();
                if(ce.inputPumper!=null) ce.inputPumper.interrupt();
                if(ce.errorPumper!=null) ce.errorPumper.interrupt();
                if( ce.fos !=null){
//                    logger.debug(" command "+ preCommand + " file output stream closed." );
                    ce.fos.close();
                }
                // remove ce from list
                bgProcesses.remove(preCommand);

//                System.out.println(preCommand+ " canceled sucessfully");
                if(ce.proc !=null) ret=ce.proc.exitValue();
            }catch(Throwable e){
                logger.error("Could not stop process "+ preCommand, e);
            }
            return ret;
        }
    }
    /**
     * Run at background
     * @return the subprocess running at background
     */
    public Process backgroundRun(String command) throws IOException {
            //log command for system check
            if(isLogging)logger.debug(command);
            // exec command on system runtime
         proc = Runtime.getRuntime().exec(command);
        //   proc = Runtime.getRuntime().exec("cmd.exe /c start "+command);

            fos=null;
            if( out!=null )  {
               // fos=new PrintWriter( new FileWriter( out ) );
            	fos=new PrintWriter(new OutputStreamWriter(new FileOutputStream(out,false),"UTF-8"));
            }

            // copy input and error to the output stream
            inputPumper =
                new StreamPumper(proc.getInputStream(), "exec",  fos);
            errorPumper =
                new StreamPumper(proc.getErrorStream(), "error", fos);

            // starts pumping away the generated output/error
            inputPumper.start();
            errorPumper.start();
            synchronized( bgProcesses){
               bgProcesses.put(command,this) ;
  //             System.out.println(command+ " running at background");

            }

        return proc;
    }
    /**
     * Run and wait for ending
     */
    public int run(String command) throws IOException {
        int err = -1; // assume the worst
        try {

            proc=backgroundRun(command);
            // Wait for everything to finish
            proc.waitFor();
            if(inputPumper!=null) inputPumper.join();
            if(errorPumper!=null) errorPumper.join();
            if( proc !=null) proc.destroy();
            
            err=stopBackgroundProcess(command);

        }  catch (InterruptedException ex) {
            logger.error("Error run "+ command , ex);
        }

        return err;
    }

    /*public void setDir(String d) {
        this.dir = resolveFile(d);
    }



    public void setOs(String os) {
        this.os = os;
    }*/

    public void setCommand(String command) {
        this.command = command;
    }

    public void setOutput(String out) {
        this.out = out;
    }

    // Inner class for continually pumping the input stream during
    // Process's runtime.
    class StreamPumper extends Thread {
        private BufferedReader din;
        private boolean endOfStream = false;
//        private int SLEEP_TIME = 200; // 200 miliseconds
        private PrintWriter fos;

        public StreamPumper(InputStream is, String name, PrintWriter fos) {
            this.din     = new BufferedReader(new InputStreamReader(is));
            this.fos     = fos;
        }
        public void pumpStream()
            throws IOException
        {
            
            if (!endOfStream) {
                String line = din.readLine();

                if (line != null) {
                    if (fos != null){
                        fos.println(line);
                        fos.flush();
                        fos.flush();
                    }
                    if(isLogging)
                    	logger.debug(line);
                    if(ps!=null ){
                    	try{ps.println(line);}catch(Throwable tx){}
                    }
                } else {
                    endOfStream = true;
                }
            }
        }

        public void run() {
            try {
//                try {
                    while (!endOfStream) {
                        pumpStream();
//                        sleep(SLEEP_TIME);
                    }
//                } catch (InterruptedException ie) {}
                din.close();
            } catch (IOException ioe) {}
        }
    }
}
