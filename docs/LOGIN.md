# Import a desktop login file

This is a one-time file-import workflow, not an official mobile Jagex sign-in. The exported file grants access to the selected account/character. Keep it private.

## On the desktop computer

These steps follow RuneLite's [Using Jagex Accounts guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

1. Install an up-to-date official RuneLite launcher (the upstream guide requires 2.6.3 or newer).
2. On Windows, open **RuneLite (configure)** from the Start menu. On Mac or Linux, start the RuneLite launcher with `--configure`. For a standard Mac installation:

   ```sh
   /Applications/RuneLite.app/Contents/MacOS/RuneLite --configure
   ```

3. In **Client arguments**, add `--insecure-write-credentials`, keeping any existing arguments. Save.
4. Open RuneLite **through the Jagex Launcher**, selecting the character you want.
5. Find `credentials.properties` in your `.runelite` folder:

   | System | Folder |
   |---|---|
   | Windows | Press Win+R, enter `%USERPROFILE%\.runelite` |
   | Mac | Finder → Go → Go to Folder → `~/.runelite` |
   | Linux | `~/.runelite` (show hidden files if necessary) |

6. Transfer that file privately to your Android device, for example over USB. Do not send it to the project maintainer or publish it.

If the file is missing, check that the argument was placed in Client arguments and that RuneLite was launched through Jagex afterward. A custom desktop home-directory setting can change the file location.

## On Android

1. Open this app and tap **Import login file**.
2. In Android's file picker, select the transferred `credentials.properties`.
3. Tap **Play** when the app shows **Login ready**.
4. Remove the transferred copy from Downloads/shared storage after import. The app keeps its own private copy.

Canceling or importing an invalid file returns to setup. Import validation checks the file's structure; it does not prove the session is still valid. If login later fails, generate and import a fresh file.

## Finish safely

Remove `--insecure-write-credentials` from the desktop configuration when you no longer need exports. Remove the exported desktop file if no longer needed. To invalidate sessions, use Jagex account settings' **End sessions** option as described by the upstream guide; deleting a file alone does not invalidate its tokens.

## Change or clear the account

While running: Back → Manage login → Import replacement → select the new file → Close app. Reopen to use it. Choosing Later keeps the current running session until the next full launch.

To remove the stored account: Back → Manage login → Clear stored login → Clear and close. The app removes its saved file and recovery copies, closes the active client and shows setup next time. Plugin settings remain. External copies of the file are not deleted.

Never include login files in Git, release assets, screenshots or bug reports. See [Security](../SECURITY.md).
