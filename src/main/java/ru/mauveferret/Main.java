package ru.mauveferret;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;


public class Main
{
    public static void main( String[] args )
    {

        String firmwarePath = "";

        //check if the COM port is valid
        try {
            SerialPort serial = new SerialPort(args[0]);
            serial.openPort();
            serial.closePort();


            try {
                String currentJar = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                firmwarePath = new File(currentJar).getParent() + File.separator + args[1];

                File bootFile = new File(firmwarePath);
                InputStream RGAfirmwareStream = new FileInputStream(bootFile);
                new FileLoader(args[0], RGAfirmwareStream, SerialPort.BAUDRATE_9600);
            } catch (Exception e) {

                    //if the file was not entered, will use our own firmware
                    InputStream RGAfirmwareStream = new ByteArrayInputStream(Firmware.qpboxL2.getBytes());
                    new FileLoader(args[0], RGAfirmwareStream, SerialPort.BAUDRATE_9600);
            }
        }
        catch (SerialPortException e){
            System.out.println("Serial Port Error: "+e.getExceptionType());
            System.out.println("Boot Failed!");
            System.out.println("---------------------");
            System.out.println("Enter a valid COM-port in a form \"java -jar scrypt.jar COM1\"");
            System.out.println("Available ports are: "+ Arrays.toString(SerialPortList.getPortNames()));
        }
    }
}
