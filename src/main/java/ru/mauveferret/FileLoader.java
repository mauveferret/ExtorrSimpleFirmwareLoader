package ru.mauveferret;

import java.util.stream.Stream;
import jssc.SerialPort;
import jssc.SerialPortException;

public class FileLoader
{

    public FileLoader(String commPort, Stream fstream, String baudRate)
    {

    }


    int increaseBaudRate;
    SerialPort serial;

    public boolean bootFail = false;

    //public Event StringEventHandler EmitMessage;
   // public Event EventHandler EmitFinished;

    boolean cancelRequested;

    /*public void RequestCancel()
    {
        try{
            serial.ReadTimeout = 1;
            doSleep(100);
            serial.Close();
        }
        catch{}
        cancelRequested = true;
    }
    */


    boolean waitForByte(byte want) throws SerialPortException {
        //outputString(String.Format("Waiting for byte: 0x{0:x}", want));

        for (int i = 0; i < 100; i++){
            byte b = serial.readBytes(1)[0];
            if (b == want){
                return true;
            } else {
                //outputString(String.Format("discarding: {0:x}", b));
                System.out.println("discarding: {0:x}"+b);
            }
        }
        System.out.println("giving up");
        return false;
    }


    boolean[] acksReceived = new boolean[100];



    String lastResponse = "";

    void waitReadIncomingBytes() throws SerialPortException {
        while(serial.getInputBufferBytesCount() == 0){
            doSleep(1); // wait for bytes to come
        }
        doSleep(100);
        readIncomingBytes();
    }

    void readIncomingBytes() throws SerialPortException {
        int num;

       /* if (cancelRequested){
            // tbd: make a custom exception for this
            throw new Exception ("Cancel Requested");
        }
        */

        while ( (num = serial.getInputBufferBytesCount()) != 0){
            for (int i = 0; i < num; i++){
                byte b = (byte) serial.ReadByte();
                if (b == '{'){
                    lastResponse = "{";
                } else if (b == '}'){
                    lastResponse += "}";
                    processResponse(lastResponse);
                } else {
                    lastResponse += (char) b;
                }
            }
        }
    }

    byte[] buf = new byte[1];

    void sendByte(byte ch)
    {
        buf[0] = (byte) ch;
        serial.Write(buf, 0, 1);
    }

    void resetQpBox()
    {
        // enough for 1 second at 9600 baud.
        for (int i = 0; i < 1000; i++)
            sendByte(0);

    }

    const int loaderSize = 2560;

    public FirmwareDownloader(String commPort, Stream str, int baudRate)
    {
        fstream = str;
        serial = new SerialPort(commPort, 9600);
        serial.ReadTimeout = 10000;
        this.increaseBaudRate = baudRate;
        serial.Open();
    }


    void doSleep(int millisecs)
    {
        Thread.Sleep(millisecs);qpbox.l2

    }

    void doDownload()
    {
        byte[] initChunks = new byte[loaderSize]; // loaderSize = 2560

        fstream.Read(initChunks, 0, loaderSize); // get boot record from qpbox.l2

        resetQpBox();

        if (waitForByte(0xAC) == false){
            serial.Close();
            return;
        }

        outputString(String.Format("Downloading level-2 boot loader chunks..."));
        serial.Write(initChunks, 0, loaderSize); // send boot record

        waitReadIncomingBytes(); // wait for "{Init=1}"

        if(increaseBaudRate != 9600){ // optionally increase baud based off of speed box
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

        byte[] packet = new byte[1296];
        while ( (fstream.Read(packet, 0, packet.Length)) != 0){
            readIncomingBytes();
            serial.Write(packet, 0, packet.Length);
        }

        if(serial.BytesToRead != 0){
            readIncomingBytes(); // read the rest of the packet acks, then good to go
        }

        doSleep(100);

        serial.Write("{Go}"); // tell the firmware to start

        try{
            string ack = serial.ReadLine(); // wait for "ok:all channels cleared\r\n"
            outputString(ack);

            outputString("Finished");
        }
        catch (TimeoutException) {
            outputString("Boot failed");
            bootFail = true;
            doSleep(2000);
        }

        serial.Close();

    }

    public void Download()
    {
        try {
            doDownload();
        } catch {
//          Console.WriteLine("exception in Download, thread exiting");
        serial.Close();
    }
        if (EmitFinished != null)
            EmitFinished(this, new EventArgs());

    }

}



class FirmwareDialog : Form
        {
        Button cancel;
        Label message;
        FirmwareDownloader downloader;
        Stream fstream;
        Button openCloseBtn;
        bool canceled = false;

public void ShowMessage(object sender, StringEventArgs e)
        {
        message.Text = e.StringValue;
        }

public void onDownloadFinished(object sender, EventArgs e)
        {
        if(!canceled & !downloader.bootFail){
        openCloseBtn.PerformClick();
        }
        this.Close();
        }

        void onCancel(object sender, EventArgs e)
        {
        canceled = true;
        downloader.RequestCancel();
        }


        void layoutForm()
        {
        Text = "Firmware Download";
        Height = 150;
        FormBorderStyle = FormBorderStyle.FixedDialog;
        ControlBox = false;

        cancel = new Button();
        cancel.Text = "Cancel";
        CancelButton = cancel;
        cancel.DialogResult = DialogResult.Cancel;
        Controls.Add(cancel);

        cancel.Click += new EventHandler(onCancel);

        int hmargin = 30;
        int vmargin = 10;

        cancel.Location = new Point(hmargin, ClientSize.Height - cancel.Height - vmargin);
        cancel.Left = ClientSize.Width - cancel.Width - hmargin;

        message = new Label();
        message.AutoSize = true;
        message.Text = "";
        message.Location = new Point(hmargin, cancel.Top / 2);
        Controls.Add(message);
        }


public FirmwareDialog(Button b, string commPort, int increaseBaud)
        {
        openCloseBtn = b;
        layoutForm();

        fstream = System.Reflection.Assembly.GetExecutingAssembly().
        GetManifestResourceStream("qpbox.l2");

        downloader = new FirmwareDownloader(commPort, fstream, increaseBaud);
        Thread t = new Thread(new ThreadStart(downloader.Download));
        downloader.EmitMessage += new StringEventHandler(this.ShowMessage);
        downloader.EmitFinished += new EventHandler(this.onDownloadFinished);
        t.Start();
        }

        }

