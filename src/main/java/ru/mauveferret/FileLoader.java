package ru.mauveferret;

import jssc.SerialPort;
import jssc.SerialPortException;
import java.io.IOException;
import java.io.InputStream;


public class FileLoader {

    SerialPort serial;
    InputStream RGAfirmwareStream;

    public FileLoader(String commPort, InputStream RGAfirmwareStream, int baudRate) throws SerialPortException {
        this.RGAfirmwareStream = RGAfirmwareStream;
        serial = new SerialPort(commPort);
        try {
            serial.openPort();
            serial.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            load();

            serial.closePort();

        }
        catch (IOException e){
            e.printStackTrace();
            System.out.println("Boot Failed!");

        }
        catch (InterruptedException e){
            e.printStackTrace();
            System.out.println("Boot Failed!");
        }
    }



    void load() throws SerialPortException, IOException, InterruptedException {

        System.out.println("Port "+serial.getPortName()+" is opened. Start loading...");
        // get boot record from qpbox.l2
        byte[] initChunk = RGAfirmwareStream.readNBytes(2560);
        // enough for 1 second at 9600 baud.
        for (int i = 0; i < 1000; i++) {serial.writeByte((byte) 0);}

        //checking the exact moment when CR+LF received. You then will have 2 sec to load firmware.
        if (!waitForByte("AC")) {
                serial.closePort();
            System.out.println("Error.Have no received 0xAC. Stopping...");
        }

        // send boot record
        serial.writeBytes(initChunk);

        waitReadIncomingBytes(); // wait for "{Init=1}"

        /*if (increaseBaudRate != 9600) { // optionally increase baud based off of speed box
            string baudPacket = String.Format("{0}{1}{2}{3}",
                    "{", "PacNum=1,Baud=", increaseBaudRate, "}");
            // baudPacket = {PacNum=1,Baud=115200}

            byte[] increaseBaud = System.Text.Encoding.ASCII.GetBytes(baudPacket);
            serial.Write(increaseBaud, 0, increaseBaud.Length);

            waitReadIncomingBytes(); // wait for "{PacNum=1}"
            serial.BaudRate = increaseBaudRate; // increase baud rate to communicate

            doSleep(100); // wait 100 ms, allow some time
            // for the baud changes to take place
        }
         */

        byte[] packet = new byte[1296];

        while (RGAfirmwareStream.available()>0){
            //System.out.println("TEST1:     "+RGAfirmwareStream.available());
            packet = RGAfirmwareStream.readNBytes(1296);
            serial.writeBytes(packet);
            printIncomingBytes();
            //waitReadIncomingBytes();
        }
        Thread.sleep(200);
        // read the rest of the packet acks, then good to go
        if (serial.getInputBufferBytesCount()>0) waitReadIncomingBytes();
        Thread.sleep(100);
        System.out.println();

        serial.writeBytes("{Go}".getBytes());
        Thread.sleep(200);

        System.out.println(serial.readString());
        System.out.println("The firmware is successfully loaded!");
        System.out.println("-------------CHECK PARAMETERS-------------------");

        serial.writeString("symbols\r");
        Thread.sleep(200);
        String params = "";
        while (serial.getInputBufferBytesCount()>0){
            params+=serial.readString(serial.getInputBufferBytesCount());
            Thread.sleep(200);
        }
        System.out.println(params);
        System.out.println("-------------PARAMETERS ENDED-------------------");
        System.out.println("Good bye and hav a nice day :)");
    }


    boolean waitForByte(String expected) {
        //outputString(String.Format("Waiting for byte: 0x{0:x}", want));

        try {
            for (int i = 0; i < 100; i++) {
                byte[] b = serial.readBytes(1);
                //System.out.println(i+" :"+bytesToHex(b));
                if (expected.contains(bytesToHex(b))) {
                    System.out.println("RGA is responding. Got 0xAC.");
                    return true;
                } else {
                    //outputString(String.Format("discarding: {0:x}", b));
                    System.out.println("discarding: {0:x}" + bytesToHex(b));
                }
            }
            System.out.println("giving up");
            return false;
        } catch (SerialPortException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }


    void waitReadIncomingBytes() throws SerialPortException, InterruptedException {
        while (serial.getInputBufferBytesCount() == 0) {
            // wait for bytes to come
           Thread.sleep(1);
        }
        Thread.sleep(100);
        printIncomingBytes();
    }

    void printIncomingBytes() throws SerialPortException {

        String lastResponse = "";
        int bytesAtPort = serial.getInputBufferBytesCount();

        while (bytesAtPort!= 0) {
            for (int i = 0; i < bytesAtPort; i++) {
                byte b = serial.readBytes(1)[0];
                //System.out.println(i+" "+bytesAtPort+" "+b);

                if (b == '{') {
                    lastResponse = "{";
                } else if (b == '}') {
                    lastResponse += "}";
                    if (lastResponse.contains("PacNum"))
                    {
                        int value = Integer.parseInt(lastResponse.replaceAll("[^0-9]", ""));

                        System.out.print(value*2+"% ");
                    }
                    else System.out.println("Response: "+lastResponse);
                } else {
                    lastResponse += (char) b;
                }
                bytesAtPort = serial.getInputBufferBytesCount();
            }
        }
    }

}


