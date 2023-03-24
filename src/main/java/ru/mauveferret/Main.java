package ru.mauveferret;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

import java.io.*;
import java.util.Arrays;


public class Main
{
    public static void main( String[] args )
    {
        int baudrate = SerialPort.BAUDRATE_115200;


        // choose the baudrate for the RGA
        if (args.length>1) {
            try {
                int userBaudrate = Integer.parseInt(args[1]);
                if (userBaudrate==SerialPort.BAUDRATE_9600 ||
                        userBaudrate==SerialPort.BAUDRATE_14400 ||
                        userBaudrate==SerialPort.BAUDRATE_19200 ||
                        userBaudrate==SerialPort.BAUDRATE_38400 ||
                        userBaudrate==SerialPort.BAUDRATE_57600 ||
                        userBaudrate==SerialPort.BAUDRATE_115200) {
                    baudrate = userBaudrate;
                }
            }
            catch (Exception ignored){}
        }
        System.out.println("The baudrate is set to be "+baudrate);


        String firmwarePath = "";
        //check if the COM port is valid
        try {
            SerialPort serial = new SerialPort(args[0]);
            try {
                serial.closePort();
            }
            catch (Exception ignored){}

            serial.openPort();
            serial.closePort();

            //check if the file_path was provided
            try {
                String currentJar = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                firmwarePath = new File(currentJar).getParent() + File.separator + args[2];
                File bootFile = new File(firmwarePath);
                InputStream RGAfirmwareStream = new FileInputStream(bootFile);
                System.out.println("your  firmware file is found!");
                new FileLoader(args[0], RGAfirmwareStream, SerialPort.BAUDRATE_9600);
            } catch (Exception e) {
                    //if the file was not entered, will use our own firmware
                    InputStream RGAfirmwareStream = new ByteArrayInputStream(Firmware.qpboxL2.getBytes());
                    new FileLoader(args[0], RGAfirmwareStream, baudrate);
            }
        }
        catch (SerialPortException e){
            System.out.println("Serial Port Error: "+e.getExceptionType());
            System.out.println("Boot Failed!");
            System.out.println("---------------------");
            System.out.println("Enter a valid COM-port in a form \"java -jar scrypt.jar COM1 baud_rate firmware_path\".");
            System.out.println("You may also try without the firmware argument. The Scrypt will use its own then.");
            System.out.println("You may even not specify the baudrate. In this case 115200 will be used");
            System.out.println("Meanwhile you can't specify firmware_path and not specify baudrate!");
            System.out.println("If you need any assistance, write to mauveferreg@gmail.com");
            System.out.println("------------------------------------------------------------------------");
            System.out.println("Available ports are: "+ Arrays.toString(SerialPortList.getPortNames()));
        }
    }
}
