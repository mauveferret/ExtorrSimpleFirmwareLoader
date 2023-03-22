package ru.mauveferret;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public class FileLoader {

    SerialPort serial;
    InputStream RGAfirmwareStream;
    int increaseBaudRate;

    public FileLoader(String commPort, InputStream RGAfirmwareStream, int baudRate) {
        this.RGAfirmwareStream = RGAfirmwareStream;
        serial = new SerialPort(commPort);
        try {
            serial.setParams(baudRate, 8, 1, 0);
            serial.openPort();
            load();

            serial.closePort();

        } catch (SerialPortException e) {
            System.out.println("Serial Port Error");
            e.printStackTrace();
            System.out.println("Boot Failed!");
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

        // get boot record from qpbox.l2
        byte[] initChunk = RGAfirmwareStream.readNBytes(2560);

        // enough for 1 second at 9600 baud.
        for (int i = 0; i < 1000; i++) {serial.writeByte((byte) 0);}


        //checking the exact moment when CR+LF received. You then will have 2 sec to load firmware.
        if (!waitForByte((byte) 0xAC)) {
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
            packet = RGAfirmwareStream.readNBytes(1296);
            serial.writeBytes(packet);
            printIncomingBytes();
        }

        // read the rest of the packet acks, then good to go
        if (serial.getInputBufferBytesCount()>0) printIncomingBytes();
        Thread.currentThread().wait(100);

        serial.writeBytes("{Go}".getBytes());

        System.out.println(serial.readString());
        System.out.println("Finished. Good bye)");

    }


    boolean waitForByte(byte expected) {
        //outputString(String.Format("Waiting for byte: 0x{0:x}", want));

        try {
            for (int i = 0; i < 100; i++) {
                byte b = serial.readBytes(1)[0];
                if (b == expected) {
                    return true;
                } else {
                    //outputString(String.Format("discarding: {0:x}", b));
                    System.out.println("discarding: {0:x}" + b);
                }
            }
            System.out.println("giving up");
            return false;
        } catch (SerialPortException e) {
            e.printStackTrace();
            return false;
        }
    }




    void waitReadIncomingBytes() throws SerialPortException, InterruptedException {
        while (serial.getInputBufferBytesCount() == 0) {
            Thread.currentThread().wait(1);// wait for bytes to come
        }
        Thread.currentThread().wait(100);
        printIncomingBytes();
    }

    void printIncomingBytes() throws SerialPortException {

        String lastResponse = "";
        int bytesAtPort = serial.getInputBufferBytesCount();

        while (bytesAtPort!= 0) {
            for (int i = 0; i < bytesAtPort; i++) {
                byte b = serial.readBytes(1)[0];
                if (b == '{') {
                    lastResponse = "{";
                } else if (b == '}') {
                    lastResponse += "}";
                    System.out.println(lastResponse);
                } else {
                    lastResponse += (char) b;
                }
            }
        }
    }

}


