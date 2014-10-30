package paxchecker;

import paxchecker.GUI.*;

/**
 *
 * @author SunnyBat
 */
public class PAXChecker {

  public static final String VERSION = "1.7.3";
  // GUIs
  protected static Setup setup;

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    System.out.println("Initializing...");
    loadPatchNotesInBackground();
    javax.swing.ToolTipManager.sharedInstance().setDismissDelay(600000); // Make Tooltips stay forever
    Email.init();
    KeyboardHandler.init();
    parseCommandLineArgs(args);
  }

  public static void parseCommandLineArgs(String[] args) {
    boolean doUpdate = true;
    boolean autoStart = false;
    boolean commandLine = false;
    if (args.length > 0) {
      System.out.println("Args!");
      boolean checkPax = true;
      boolean checkShowclix = true;
      argsCycle:
      for (int a = 0; a < args.length; a++) {
        System.out.println("args[" + a + "] = " + args[a]);
        switch (args[a].toLowerCase()) {
          case "-noupdate":
            // Used by the program when starting the new version just downloaded. Can also be used if you don't want updates
            doUpdate = false;
            break;
          case "-typelink":
            KeyboardHandler.setTypeLink(true);
            break;
          case "-email":
            Email.setUsername(args[a + 1]);
            System.out.println("Username set to " + Email.getUsername());
            break;
          case "-password":
            Email.setPassword(args[a + 1]);
            System.out.println("Password set");
            break;
          case "-cellnum":
            for (int b = a + 1; b < args.length; b++) {
              if (args[b].startsWith("-")) {
                a = b - 1;
                continue argsCycle;
              }
              System.out.println("Adding email address " + args[b]);
              Email.addEmailAddress(args[b]);
            }
            break;
          case "-expo":
            Browser.setExpo(args[a + 1]);
            System.out.println("Expo set to " + Browser.getExpo());
            break;
          case "-nopax":
            System.out.println("Setting check PAX website to false");
            checkPax = false;
            break;
          case "-noshowclix":
            System.out.println("Setting check Showclix website to false");
            checkShowclix = false;
            break;
          case "-alarm":
            System.out.println("Alarm activated");
            Audio.setPlayAlarm(true);
            break;
          case "-delay":
            Checker.setRefreshTime(Integer.getInteger(args[a + 1], 15));
            System.out.println("Set refresh time to " + Checker.getRefreshTime());
            break;
          case "-autostart":
            autoStart = true;
            break;
          case "-cli":
            commandLine = true;
            break;
          default:
            if (args[a].startsWith("-")) {
              System.out.println("Unknown argument: " + args[a]);
            }
            break;
        }
      }
      if (checkPax) {
        Browser.enablePaxWebsiteChecking();
      }
      if (checkShowclix) {
        Browser.enableShowclixWebsiteChecking();
      }
      if (autoStart && !Browser.isCheckingPaxWebsite() && !Browser.isCheckingShowclix()) {
        System.out.println("ERROR: Program is not checking PAX or Showclix website. Program will now exit.");
        System.exit(0);
      }
    }
    if (commandLine) {
      ErrorHandler.setCommandLine(true);
      if (doUpdate) {
        UpdateHandler.autoUpdate(args);
      }
      Checker.commandLineSettingsInput();
      Checker.startCommandLineWebsiteChecking();
      return;
    }
    if (doUpdate) {
      UpdateHandler.checkUpdate(args);
    }
    if (autoStart) {
      Checker.startCheckingWebsites();
    } else {
      setup = new Setup();
    }
    Checker.loadAlertIcon();
  }

  /**
   * Creates a new non-daemon Thread with the given Runnable object.
   *
   * @param run The Runnable object to use
   */
  public static void continueProgram(Runnable run) {
    Thread newThread = new Thread(run);
    newThread.setName("Program Loop");
    newThread.setDaemon(false); // Prevent the JVM from stopping due to zero non-daemon threads running
    newThread.setPriority(Thread.NORM_PRIORITY);
    newThread.start(); // Start the Thread
  }

  /**
   * This makes a new daemon, low-priority Thread and runs it.
   *
   * @param run The Runnable to make into a Thread and run
   */
  public static void startBackgroundThread(Runnable run) {
    startBackgroundThread(run, "General Background Thread");
  }

  /**
   * Starts a new daemon Thread.
   *
   * @param run The Runnable object to use
   * @param name The name to give the Thread
   */
  public static void startBackgroundThread(Runnable run, String name) {
    Thread newThread = new Thread(run);
    newThread.setName(name);
    newThread.setDaemon(true); // Kill the JVM if only daemon threads are running
    newThread.setPriority(Thread.MIN_PRIORITY); // Let other Threads take priority, as this will probably not run for long
    newThread.start(); // Start the Thread
  }

  /**
   * Loads the Patch Notes on a new daemon Thread. This also sets the Patch Notes in the Setup window if possible.
   */
  public static void loadPatchNotesInBackground() {
    startBackgroundThread(new Runnable() {
      @Override
      public void run() {
        UpdateHandler.loadVersionNotes();
        if (UpdateHandler.getVersionNotes() != null && setup != null) {
          setup.setPatchNotesText(UpdateHandler.getVersionNotes());
        }
      }
    }, "Load Patch Notes");
  }

  /**
   * Sends a test email. Uses the same Thread, blocks for about 10 seconds.
   */
  public static void sendTestEmail() {
    Email.testEmail();
  }

  /**
   * Sends a test email on a daemon Thread. Note that this also updates the Status window if possible.
   */
  public static void sendBackgroundTestEmail() {
    startBackgroundThread(new Runnable() {
      @Override
      public void run() {
        try {
          Checker.setStatusTextButtonState(false);
          Checker.setStatusTextButtonText("Sending...");
          if (!Email.testEmail()) {
            Checker.setStatusTextButtonText("Test Text");
            Checker.setStatusTextButtonState(true);
            return;
          }
          long timeStarted = System.currentTimeMillis();
          while (System.currentTimeMillis() - timeStarted < 60000) {
            Checker.setStatusTextButtonText((60 - (int) ((System.currentTimeMillis() - timeStarted) / 1000)) + "");
            Thread.sleep(200);
          }
          Checker.setStatusTextButtonText("Test Text");
          Checker.setStatusTextButtonState(true);
        } catch (Exception e) {
          System.out.println("ERROR sending background test email!");
          e.printStackTrace();
          Checker.setStatusTextButtonText("Test Text");
          Checker.setStatusTextButtonState(true);
        }
      }
    }, "Send Test Email");
  }
}
