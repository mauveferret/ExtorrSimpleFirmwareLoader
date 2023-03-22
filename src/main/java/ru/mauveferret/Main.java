package ru.mauveferret;

import jssc.SerialPort;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * Hello world!
 *
 */
public class Main
{
    public static void main( String[] args )
    {
        String firmwarePath = "";

        try {
            String currentJar = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            firmwarePath = new File(currentJar).getParent()+File.separator+args[1];

            File bootFile = new File(firmwarePath);
            InputStream RGAfirmwareStream = new FileInputStream(bootFile);
            new FileLoader(args[0], RGAfirmwareStream, SerialPort.BAUDRATE_9600);
        }
        catch (FileNotFoundException e){
            System.out.println(" File not Found: "+firmwarePath);
        }
        catch (URISyntaxException e){System.out.println(e.getMessage());}

    }
}
