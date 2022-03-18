package lolmute.main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;

import static java.lang.Thread.sleep;


public class AutoMute{

    private static boolean isMutingMates = false;
    private static boolean isMutingEnemies = false;
    private static boolean isMuted = false;
    private static boolean isStop = false;
    private static boolean isClientOpened = false;
    private static final int MAX_TITLE_LENGTH = 1024;
    private static Properties prop = new Properties();


    private static String configfile_path = System.getenv("APPDATA") + "/AutoMute/automute.cfg";

    public static void main(String[] args) throws Exception {


        if(!checkConfig()){createConfig();}

        //Nastaveni TrayIcon
        java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
        URL imgurl = new URL("https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/120/twitter/322/zipper-mouth-face_1f910.png");
        java.awt.Image img = ImageIO.read(imgurl);
        PopupMenu menu = new PopupMenu();
        MenuItem status = new MenuItem("    Status");
        CheckboxMenuItem mute_enemy = new CheckboxMenuItem("    Mute Enemy");
        mute_enemy.setState(true);
        CheckboxMenuItem mute_team = new CheckboxMenuItem("    Mute Team");
        CheckboxMenuItem unmute_pings = new CheckboxMenuItem("  Unmute Pings");
        MenuItem exit = new MenuItem("    Exit");
        ActionListener item2listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if(!mute_enemy.getState())
                {
                    writeConfig("mute_enemy","false");
                }
                else if(mute_enemy.getState())
                {
                    writeConfig("mute_enemy", "true");
                }

                if(!mute_team.getState())
                {
                    writeConfig("mute_team", "false");
                }
                else if(mute_team.getState())
                {
                    writeConfig("mute_team", "true");
                }

                if(!unmute_pings.getState())
                {
                    writeConfig("unmute_pings", "false");
                }
                else if(unmute_pings.getState())
                {
                    writeConfig("unmute_pings", "true");
                }

                System.exit(0);
            }
        };
        exit.addActionListener(item2listener);
        menu.add(status);
        menu.addSeparator();
        menu.add(mute_enemy);
        menu.add(mute_team);
        menu.add(unmute_pings);
        menu.addSeparator();
        menu.add(exit);

        try{
            prop.load(new FileInputStream(configfile_path));

            if(prop.getProperty("mute_enemy").equalsIgnoreCase("true"))
            {
                mute_enemy.setState(true);
            }
            else if(prop.getProperty("mute_enemy").equalsIgnoreCase("false"))
            {
                mute_enemy.setState(false);
            }

            if(prop.getProperty("mute_team").equalsIgnoreCase("true"))
            {
                mute_team.setState(true);
            }
            else if(prop.getProperty("mute_team").equalsIgnoreCase("false"))
            {
                mute_team.setState(false);
            }

            if(prop.getProperty("unmute_pings").equalsIgnoreCase("true"))
            {
                unmute_pings.setState(true);
            }
            else if(prop.getProperty("unmute_pings").equalsIgnoreCase("false"))
            {
                unmute_pings.setState(false);
            }

        }catch (NullPointerException e){}

        java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(img, "AutoMute - League of Legends", menu);
        trayIcon.setImageAutoSize(true);
        tray.add(trayIcon);

        String lolMatchName = "League of Legends.exe";
        String lolClientName = "League of Legends (TM) Client";

        status.setLabel("   Waiting for LoL match");

        Thread thread = new Thread(){
            @Override public void run(){
                try{
                while(!isStop)
                {
                    sleep(5000);
                        if(!status.getLabel().equalsIgnoreCase("   Waiting for LoL match"))
                        {
                            status.setLabel("   Waiting for LoL match");
                        }
                        if(checkProcess(lolMatchName))
                        {
                            if(!status.getLabel().equalsIgnoreCase("   In Match"))
                            {
                                status.setLabel("   In Match");

                            }
                            Dimension res = Toolkit.getDefaultToolkit().getScreenSize();
                            int x = res.width-150;
                            int y = 15;
                            while(!isMuted)
                            {
                                sleep(2000);
                                System.out.println("Checking on pixel " + x + ", " + y);
                                if(checkPixel(x,y,207,183,108))
                                {
                                    if(mute_enemy.getState())
                                    {
                                        System.out.println("Muting Enemy");
                                        MuteEnemy();
                                    }
                                    //Thread.sleep(1000);
                                    if(mute_team.getState())
                                    {
                                        System.out.println("Muting Team");
                                        MuteTeam();
                                        if(unmute_pings.getState())
                                        {
                                            UnmutePings();
                                        }
                                    }
                                    isMuted = true;
                                }
                            }


                        }else{
                            isMuted = false;

                        }


                }
                }catch (Exception e) {e.printStackTrace();}
            }
        };
        thread.setDaemon(true);
        thread.start();

    }

    static Boolean checkProcess(String processName)
    {
        String filenameFilter = "/nh /fi \"Imagename eq "+processName+"\"";
        String tasksCmd = System.getenv("windir") +"/system32/tasklist.exe "+filenameFilter;

        Process p;
        try {
            p = Runtime.getRuntime().exec(tasksCmd);

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            ArrayList<String> procs = new ArrayList<String>();
            String line = null;
            while ((line = input.readLine()) != null)
                procs.add(line);

            input.close();

            Boolean processFound = procs.stream().filter(row -> row.indexOf(processName) > -1).count() > 0;
            return processFound;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    static void writeConfig(String title, String value)
    {
        try {
            System.out.println("Zapisuju " + title + " " + value);
            prop.setProperty(title,value);
            prop.store(new FileOutputStream(configfile_path),null);
        }catch (Exception e){e.printStackTrace();}
    }


    static void createConfig()
    {
        String path = System.getenv("APPDATA") + "/AutoMute/";
        File config_folder = new File(path);
        config_folder.mkdir();
        File config_file = new File(path + "automute.cfg");
        try {
            config_file.createNewFile();
        } catch (IOException e) {e.printStackTrace();}

    }

    static boolean checkConfig()
    {
        String appdata_path = System.getenv("APPDATA");
        Path config_path = Paths.get(appdata_path+ "/AutoMute/");
        if(Files.exists(config_path))
        {
            Path config_file = Paths.get(config_path + "automute.cfg");
            if(Files.exists(config_file))
            {
                return true;
            }
        }
        return false;
    }

    static boolean checkPixel(int x,int y, int r,int g, int b) throws AWTException
    {
        Robot rbt = new Robot();
        Color pixel = rbt.getPixelColor(x,y);
        System.out.println("RGB= " + pixel.getRed() + " " + pixel.getGreen() + " " + pixel.getBlue());
        if(pixel.getRed() == r && pixel.getGreen() == g && pixel.getBlue() == b)
        {
            return true;
        }
        return false;
    }
    static void UnmutePings() throws AWTException
    {
        Dimension res = Toolkit.getDefaultToolkit().getScreenSize();

        Robot rbt = new Robot();

        rbt.setAutoDelay(50);

        rbt.keyPress(KeyEvent.VK_TAB);

        int pos_x = res.width-1024;
        int pos_y = res.height-760;
        for(int i = 1;i<=5;i++)
        {
            System.out.println("unmuting blue-side ally #" + i + " on coords - x=" + pos_x + " y=" + pos_y);
            if(i == 1)
            {
                if(checkPixel(pos_x,pos_y, 9,17,14))
                {
                    continue;
                }
            }
            rbt.mouseMove(pos_x,pos_y);
            rbt.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            rbt.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            pos_y+=76;
        }


        int pos_x_redteam = res.width-454;
        int pos_y_redteam = res.height-760;
        for(int k = 1;k<=5;k++)
        {
            System.out.println("unmuting red-side ally #" + k  + " on coords - x=" + pos_x_redteam + " y=" + pos_y_redteam);
            rbt.mouseMove(pos_x_redteam,pos_y_redteam);
            rbt.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            rbt.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            pos_y_redteam+=76;
        }


        rbt.keyRelease(KeyEvent.VK_TAB);
    }

    static void MuteTeam() throws  AWTException{
        Robot rbt = new Robot();

        rbt.setAutoDelay(10);

        rbt.mouseMove(250, 250);
        rbt.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        rbt.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        rbt.keyPress(KeyEvent.VK_ENTER);
        rbt.keyRelease(KeyEvent.VK_ENTER);

        rbt.keyPress(KeyEvent.VK_DIVIDE);
        rbt.keyRelease(KeyEvent.VK_DIVIDE);
        rbt.keyPress(KeyEvent.VK_M);
        rbt.keyRelease(KeyEvent.VK_M);
        rbt.keyPress(KeyEvent.VK_U);
        rbt.keyRelease(KeyEvent.VK_U);
        rbt.keyPress(KeyEvent.VK_T);
        rbt.keyRelease(KeyEvent.VK_T);
        rbt.keyPress(KeyEvent.VK_E);
        rbt.keyRelease(KeyEvent.VK_E);

        rbt.keyPress(KeyEvent.VK_SPACE);
        rbt.keyRelease(KeyEvent.VK_SPACE);

        rbt.keyPress(KeyEvent.VK_T);
        rbt.keyRelease(KeyEvent.VK_T);
        rbt.keyPress(KeyEvent.VK_E);
        rbt.keyRelease(KeyEvent.VK_E);
        rbt.keyPress(KeyEvent.VK_A);
        rbt.keyRelease(KeyEvent.VK_A);
        rbt.keyPress(KeyEvent.VK_M);
        rbt.keyRelease(KeyEvent.VK_M);

        rbt.keyPress(KeyEvent.VK_ENTER);
        rbt.keyRelease(KeyEvent.VK_ENTER);
    }

    static void MuteEnemy() throws  AWTException{
        Robot rbt = new Robot();
        rbt.setAutoDelay(10);

        rbt.mouseMove(250, 250);
        rbt.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        rbt.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        rbt.keyPress(KeyEvent.VK_ENTER);
        rbt.keyRelease(KeyEvent.VK_ENTER);

        rbt.keyPress(KeyEvent.VK_DIVIDE);
        rbt.keyRelease(KeyEvent.VK_DIVIDE);
        rbt.keyPress(KeyEvent.VK_M);
        rbt.keyRelease(KeyEvent.VK_M);
        rbt.keyPress(KeyEvent.VK_U);
        rbt.keyRelease(KeyEvent.VK_U);
        rbt.keyPress(KeyEvent.VK_T);
        rbt.keyRelease(KeyEvent.VK_T);
        rbt.keyPress(KeyEvent.VK_E);
        rbt.keyRelease(KeyEvent.VK_E);
        rbt.keyPress(KeyEvent.VK_SPACE);
        rbt.keyRelease(KeyEvent.VK_SPACE);
        rbt.keyPress(KeyEvent.VK_E);
        rbt.keyRelease(KeyEvent.VK_E);
        rbt.keyPress(KeyEvent.VK_N);
        rbt.keyRelease(KeyEvent.VK_N);
        rbt.keyPress(KeyEvent.VK_E);
        rbt.keyRelease(KeyEvent.VK_E);
        rbt.keyPress(KeyEvent.VK_M);
        rbt.keyRelease(KeyEvent.VK_M);
        rbt.keyPress(KeyEvent.VK_Y);
        rbt.keyRelease(KeyEvent.VK_Y);

        rbt.keyPress(KeyEvent.VK_ENTER);
        rbt.keyRelease(KeyEvent.VK_ENTER);
    }
}
