/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package raceresults;

/**
 *
 * @author Bob
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RaceResults {

    private static String ReqPrevResult = "x";
    private static String ReqThisBarrier = null;
    private static String ReqeMail = null;
    private static String OutputFileName = null;
    private static int RaceCount = 0;
    private static int HorseCount = 0;
    private static int GoodCount = 0;
    private static int MeetingsCount = 0;
    private static final Boolean APPEND = true;
    private static final Boolean REPLACE = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        String ReqURL;
        String iFileNameText;
        String FrontPageString;
        String MeetingsLine;
        String initialContext = "";
        String YesterdayDateString;

        String HeaderString = "Meeting	Race	Time	Horse #	Previous	Horse Name	Barrier	Trainer	Jockey	Weight	SportsBet	Ladbrokes";

        Date StartDate;
        Date EndDate;
        long TimeRunSecs;

        if (args.length > 0) {
            try {
                StartDate = getCurrDate();
                YesterdayDateString = getYesterdayDateString();
                iFileNameText = args[0];

                // get parameters from args[0]
                ReqURL = getParameter(iFileNameText, "URL");
                OutputFileName = getParameter(iFileNameText, "ResultFile");

                // get the front page to get the URLs for the meetings
                FrontPageString = getPage(ReqURL);

                // get the line with all the meetings
                MeetingsLine = splitpage(FrontPageString);
//                WriteFile(OutputFileName, REPLACE, MeetingsLine);
                splitmeetingsline(MeetingsLine, YesterdayDateString);

                // produce timing of job
                EndDate = getCurrDate();
                TimeRunSecs = (EndDate.getTime() - StartDate.getTime()) / 1000;
                System.out.println("Time to run " + TimeRunSecs);

            } catch (Exception ex) {
                System.out.println("RaceResults.jar has an error: " + ex);
            }
        } else {
            System.out.println("You must supply the input file name.");
        }

    }  // end of main

    // *********************************************************************************************************************
    public static Date getCurrDate() {
        //getting current date and time using Date class
        Date dateobj = new Date();
        return dateobj;
    }

    // *********************************************************************************************************************
    // get yesterdays date in format yyyy-mm-dd
    private static String getYesterdayDateString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return dateFormat.format(cal.getTime());
    }

    // *********************************************************************************************************************
    public static String getParameter(String iFileNameText, String ParameterText) {
        // Open the file
        FileInputStream fstream = null;
        String ReturnParameter = "not found";

        try {
            fstream = new FileInputStream(iFileNameText);
        } catch (FileNotFoundException ex) {
            System.out.println(iFileNameText + " has an error: " + ex);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(fstream))) {
            String strLine;
//Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                // find token
                String delims = "=";
                String[] tokens = strLine.split(delims);
                if (tokens[0].equals(ParameterText)) {
                    ReturnParameter = tokens[1];
                }
            }
            //Close the input stream
            br.close();
        } catch (IOException ex) {
            System.out.println(iFileNameText + " has an error: " + ex);
        }
        return ReturnParameter;
    }

    // *********************************************************************************************************************
    // get the front page into a string
    public static String getPage(String webPage) {
        String result = null;

        try {
            URL url = new URL(webPage);
            URLConnection urlConnection = url.openConnection();
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuilder sb = new StringBuilder();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                sb.append(charArray, 0, numCharsRead);
            }
            result = sb.toString();

        } catch (MalformedURLException ex) {
//            e.printStackTrace();
            System.out.println(webPage + " has an error: " + ex);
        } catch (IOException ex) {
            System.out.println(webPage + " has an error: " + ex);
//            e.printStackTrace();
        }
        return result;
    }

    // *********************************************************************************************************************
    // split the front web page to get the meetings line
    public static String splitpage(String webpagestring) {
        String MeetingsLine = null;

        // Split string based on carriage returns line feeds.
        String delims = "\\r\\n";
        String[] tokens = webpagestring.split(delims);

        for (String token : tokens) {
            if (token.contains("pastRaceResultsContainer")) {
                MeetingsLine = token;
            }
        }

        return MeetingsLine;

    }

    // *********************************************************************************************************************
    // write information to output file
    public static void WriteFile(String OutputFileName, Boolean APPEND, String Text) {
        try {
            FileWriter writer = new FileWriter(OutputFileName, APPEND);
            try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
                bufferedWriter.write(Text);
                bufferedWriter.newLine();

                //Close writer
                bufferedWriter.flush();
            }

        } catch (IOException ex) {
            //           Logger.getLogger(Race42.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Writing to file " + OutputFileName + " has an error: " + ex);
        }

    }

// *********************************************************************************************************************
    // split the meetings line into the various individual meeting lines
    public static void splitmeetingsline(String MeetingsLine, String YesterdayDateString) {

        // Split string based on "form_meeting".
        String[] tokens = MeetingsLine.split("</a>");

        for (String token : tokens) {
            if (token.contains(YesterdayDateString)) {  // yesterdays meetings
                extractURL(token, YesterdayDateString);
//                System.out.println(token);
                MeetingsCount = MeetingsCount + 1;
            }
        }
    }

    // *********************************************************************************************************************
    // get the race fields URLs from the meeting line
    public static void extractURL(String MeetingLine, String YesterdayDateString) {
        String URLstring = null;
        String formpage;

        // Split string based on '.
        String[] tokens = MeetingLine.split("'");

        for (String token : tokens) {
            if (token.contains(YesterdayDateString)) {
                URLstring = "https://www.racenet.com.au" + token;
                System.out.println(URLstring);
            }
        }

        //       formpage = getPage(URLstring);
//        extractVENUE(formpage);
    }

}
