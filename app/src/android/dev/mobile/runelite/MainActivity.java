package dev.mobile.runelite;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.system.Os;
import android.view.*;
import android.view.inputmethod.*;
import android.text.InputType;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int IMPORT = 10, EXPORT = 11;
    private int WIDTH = 1100, HEIGHT = 540;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView status, launchStatus;
    private Button start;
    private LinearLayout toolbar;
    private AlertDialog controls;
    private SwingView screen;
    private SurfaceView gameSurface;
    private FrameLayout gameLayers;
    private File base, home, log;
    private volatile boolean drawing, started;
    private GameInterfaceControls gameInterface;
    private int mouseMask = 1024;
    private boolean automaticDrag = true;
    private boolean reverseCamera;
    private float cameraSensitivity=1f;
    private AlertDialog loginSetup;
    private boolean keyboardShown;
    private String hdProfile;
    private final String[] profileNames={"low","medium","high"};
    private final String[] profileLabels={"Low","Medium","High"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        android.content.SharedPreferences viewport = getSharedPreferences("viewport", MODE_PRIVATE);
        WIDTH = viewport.getInt("width", 1100); HEIGHT = viewport.getInt("height", 540);
        android.content.SharedPreferences startup=getSharedPreferences("startup",MODE_PRIVATE);
        hdProfile=MobileHdProfiles.normalize(startup.getString("hdProfile","low"));
        startup.edit().putString("hdProfile",hdProfile).apply();
        base = new File(getFilesDir(), "runtime-v1");
        home = new File(getFilesDir(), "home"); home.mkdirs();
        log = new File(home, "startup.log");
        NativeBridge.init(getApplicationContext());
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        toolbar = new LinearLayout(this); toolbar.setOrientation(LinearLayout.VERTICAL);
        start = new Button(this); // Startup state only; no redundant in-game Start control.
        gameInterface=new GameInterfaceControls(this);
        button(toolbar,"Rendering fallback",this::showPresentationOptions);
        button(toolbar,"Screen layout",()->gameInterface.showLayout());
        button(toolbar,"Send text",this::type);
        Button mouse=button(toolbar,"Drag: automatic camera",()->{});
        mouse.setOnClickListener(v->{automaticDrag=!automaticDrag;mouse.setText(automaticDrag?"Drag: automatic camera":"Drag: inventory/interface");});
        button(toolbar,"Escape",()->key(27));
        button(toolbar,"Enter",()->key(10));
        button(toolbar,"Backspace",()->key(8));
        CheckBox reverse = new CheckBox(this); reverse.setText("Reverse horizontal camera drag");
        reverseCamera = viewport.getBoolean("reverseCamera",false); reverse.setChecked(reverseCamera);
        reverse.setOnCheckedChangeListener((v,checked) -> {
            reverseCamera=checked; viewport.edit().putBoolean("reverseCamera",checked).apply();
        }); toolbar.addView(reverse);
        cameraSensitivity=viewport.getInt("cameraSensitivity",100)/100f;
        TextView speedLabel=new TextView(this);speedLabel.setText("Camera speed: "+Math.round(cameraSensitivity*100)+"%");toolbar.addView(speedLabel);
        SeekBar speed=new SeekBar(this);speed.setMax(150);speed.setProgress(Math.round(cameraSensitivity*100)-50);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar,int value,boolean user) {
                cameraSensitivity=(value+50)/100f;speedLabel.setText("Camera speed: "+(value+50)+"%");
                if(user) viewport.edit().putInt("cameraSensitivity",value+50).apply();
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        });toolbar.addView(speed);
        button(toolbar, "Diagnostic logs", this::showLogs);
        // Controls live in a dialog; the game owns the entire activity surface.
        status = new TextView(this); status.setPadding(12, 4, 12, 4);
        status.setText("117HD mobile • tap: click • hold: menu • drag: camera • pinch: zoom");
        toolbar.addView(status);
        
        screen = new SwingView(this);
        gameLayers=new FrameLayout(this);gameLayers.setClipChildren(true);
        gameSurface=new SurfaceView(this);gameSurface.setZOrderOnTop(false);
        gameSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {NativeBridge.surface(holder.getSurface());}
            public void surfaceChanged(SurfaceHolder holder,int format,int width,int height) { /* Same native window; render thread owns EGL. */ }
            public void surfaceDestroyed(SurfaceHolder holder) {NativeBridge.surface(null);}
        });
        gameLayers.addView(gameSurface,new FrameLayout.LayoutParams(1,1));
        gameLayers.addView(screen,new FrameLayout.LayoutParams(-1,-1));
        launchStatus=new TextView(this);launchStatus.setGravity(Gravity.CENTER);launchStatus.setTextSize(18);launchStatus.setTextColor(Color.WHITE);launchStatus.setBackgroundColor(Color.rgb(25,25,25));launchStatus.setVisibility(View.GONE);
        gameLayers.addView(launchStatus,new FrameLayout.LayoutParams(-1,-1));
        layout.addView(gameLayers, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }
        for (int i = 0; i < toolbar.getChildCount(); i++)
            toolbar.getChildAt(i).setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        start.setOnClickListener(v -> { launch(); if (controls != null) controls.dismiss(); });
        if (!NativeBridge.running) screen.post(this::showLoginSetup);
        if (NativeBridge.running) {
            started = true; start.setEnabled(false); startFrames();
        }
    }

    private Button button(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setTextSize(12);
        button.setMinWidth(0); button.setMinimumWidth(0); button.setPadding(8, 0, 8, 0);
        button.setOnClickListener(v -> {dismissControls();action.run();});
        parent.addView(button, new LinearLayout.LayoutParams(0, -2, 1)); return button;
    }
    private void message(String value) {ui.post(()-> {
        status.setText(value);
        if(launchStatus!=null && launchStatus.getVisibility()==View.VISIBLE)launchStatus.setText(value+"\n\nFirst launch can take longer. Back opens app controls.");
        if(value.startsWith("Startup failed") || value.startsWith("RuneLite startup failed"))
            new AlertDialog.Builder(this).setTitle("Could not start RuneLite").setMessage("Open Help & advanced → Diagnostic logs for details, or close and reopen to retry.")
                .setPositiveButton("Logs",(d,w)->showLogs()).setNegativeButton("Close app",(d,w)->closeClient()).show();
    });}

    private void chooseHdProfile(String profile) {
        hdProfile=profile;
        getSharedPreferences("startup",MODE_PRIVATE).edit().putString("hdProfile",profile)
            .putLong("hdProfileRevision",System.currentTimeMillis()).apply();
    }
    private void manageLogin() {
        if(!started) {showLoginSetup();return;}
        new AlertDialog.Builder(this).setTitle("Manage login")
            .setMessage("Import a replacement login, or clear the stored login. Account changes take effect after closing and reopening the app.")
            .setPositiveButton("Import replacement",(d,w)->importLogin())
            .setNeutralButton("Clear stored login",(d,w)->confirmClearLogin())
            .setNegativeButton("Cancel",null).show();
    }
    private void confirmClearLogin() {
        new AlertDialog.Builder(this).setTitle("Clear stored login?")
            .setMessage(started?"This removes the imported login from this app and closes RuneLite to discard the active login. Reopen to import another account. Your plugins and settings are kept.":
                "This removes the imported login from this app. You can then import another account. Your plugins and settings are kept.")
            .setPositiveButton(started?"Clear and close":"Clear login",(d,w)-> {
                try {
                    StoredLogin.clear(home);
                    getSharedPreferences("startup",MODE_PRIVATE).edit().putBoolean("skipSetup",false).commit();
                    if(started) {drawing=false;finishAndRemoveTask();android.os.Process.killProcess(android.os.Process.myPid());}
                    else {if(loginSetup!=null)loginSetup.dismiss();showLoginSetup();}
                } catch(IOException failure) {Toast.makeText(this,"Could not clear the stored login. No account switch was started.",Toast.LENGTH_LONG).show();}
            }).setNegativeButton("Cancel",null).show();
    }
    private void importLogin() {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
            .addCategory(Intent.CATEGORY_OPENABLE), IMPORT);
    }
    private void showLoginHelp() {
        TextView help=new TextView(this);help.setPadding(24,12,24,12);help.setTextIsSelectable(true);
        help.setText("On your computer:\n\n1. Open RuneLite (configure) from the Windows Start menu. On Mac/Linux, launch RuneLite with --configure.\n2. Add --insecure-write-credentials in Client arguments and save.\n3. Open RuneLite through the Jagex Launcher with the character you want.\n4. Copy credentials.properties from your .runelite folder to this device.\n\nWindows folder: %USERPROFILE%\\.runelite\nMac/Linux folder: ~/.runelite\n\nBack here, tap Import login file and select that file.\n\nThis file grants account access. Keep it private. After importing, remove the copied file from Downloads and the desktop export option when no longer needed. Clearing this app's login does not revoke Jagex sessions.");
        ScrollView scroll=new ScrollView(this);scroll.addView(help);
        new AlertDialog.Builder(this).setTitle("Get your login file").setView(scroll)
            .setPositiveButton("Done",null).setNeutralButton("RuneLite guide",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts")))).show();
    }
    private void showLoginSetup() {
        if(started)return;
        if(getSharedPreferences("startup",MODE_PRIVATE).getBoolean("skipSetup",false)) {launch();return;}
        if(loginSetup!=null && loginSetup.isShowing())return;
        boolean imported=new File(home,".runelite/credentials.properties").isFile();
        loginSetup=new AlertDialog.Builder(this).setTitle(imported?"Login ready":"Welcome to Runelite Mobile (HD)")
            .setMessage(imported?"Your login file is saved on this device. Tap Play to start. You can change accounts and graphics later from the Back menu.":
                "For a Jagex Account, import your own desktop RuneLite login file once. Need the file? Tap Help. You can change graphics later from the Back menu.")
            .setPositiveButton(imported?"Play":"Import login file",(d,w)-> {
                if(imported) {getSharedPreferences("startup",MODE_PRIVATE).edit().putBoolean("skipSetup",true).apply();launch();}
                else importLogin();
            }).setNeutralButton("Help",(d,w)-> {loginSetup.dismiss();showLoginSetup();showLoginHelp();})
            .setNegativeButton(imported?"Change login":"Other login",(d,w)-> {
                if(imported)importLogin();
                else new AlertDialog.Builder(this).setTitle("Continue without a login file?")
                    .setMessage("This opens the game's login screen. Jagex Accounts still need an imported login file. You can import later from Back → Manage login.")
                    .setPositiveButton("Continue",(dialog,which)-> {getSharedPreferences("startup",MODE_PRIVATE).edit().putBoolean("skipSetup",true).apply();launch();})
                    .setNegativeButton("Back",(dialog,which)->showLoginSetup()).setOnCancelListener(dialog->showLoginSetup()).show();
            }).setCancelable(false).create();
        loginSetup.show();
    }
    private void dismissControls() {if(controls!=null)controls.dismiss();}
    private void closeClient() {drawing=false;finishAndRemoveTask();android.os.Process.killProcess(android.os.Process.myPid());}
    private void showGraphicsOptions() {
        int selected=Arrays.asList(profileNames).indexOf(hdProfile);
        new AlertDialog.Builder(this).setTitle("Graphics quality")
            .setSingleChoiceItems(profileLabels,Math.max(0,selected),(dialog,which)-> {
                chooseHdProfile(profileNames[which]);dialog.dismiss();
                new AlertDialog.Builder(this).setTitle("Graphics saved").setMessage("Close and reopen the app to apply this quality setting.")
                    .setPositiveButton("Close app",(d,w)->closeClient()).setNegativeButton("Later",null).show();
            }).setNegativeButton("Cancel",null).show();
    }
    private void showPresentationOptions() {
        String[] labels={"Direct Surface (recommended)","Copied frames — pipelined","Copied frames — synchronous"};
        String selected=getSharedPreferences("startup",MODE_PRIVATE).getString("glesPresentation","surface");
        new AlertDialog.Builder(this).setTitle("Rendering fallback").setMessage("Change only if the game display is broken. Close and reopen afterward.")
            .setSingleChoiceItems(labels,"sync".equals(selected)?2:"copy".equals(selected)?1:0,(dialog,which)-> {
                getSharedPreferences("startup",MODE_PRIVATE).edit().putString("glesPresentation",which==2?"sync":which==1?"copy":"surface").apply();dialog.dismiss();
                Toast.makeText(this,"Saved. Close and reopen to apply.",Toast.LENGTH_LONG).show();
            }).setNegativeButton("Cancel",null).show();
    }
    private void launch() {
        if (started) return;
        if(screen.getWidth()<=0 || screen.getHeight()<=0) {screen.post(this::launch);return;}
        int[] viewportSize=ViewportSizing.calculate(screen.getWidth(),screen.getHeight(),false,getSharedPreferences("startup",MODE_PRIVATE).getInt("layoutHeight",540));
        WIDTH=viewportSize[0];HEIGHT=viewportSize[1];
        getSharedPreferences("viewport", MODE_PRIVATE).edit().putInt("width", WIDTH).putInt("height", HEIGHT).apply();
        started = true; NativeBridge.running = true;launchStatus.setVisibility(View.VISIBLE);
        start.setEnabled(false); message("Preparing bundled Java runtime and RuneLite…");
        new Thread(() -> {
            try {
                prepare();
                android.util.Log.i("RuneLiteMobile","Device="+Build.MANUFACTURER+" "+Build.MODEL+" Android="+Build.VERSION.RELEASE+" viewport="+WIDTH+"x"+HEIGHT+" preset="+hdProfile);
                System.out.println("Device="+Build.MANUFACTURER+" "+Build.MODEL+" viewport="+WIDTH+"x"+HEIGHT+" preset="+hdProfile);
                List<String> options = new ArrayList<>();
                JSONArray defaults = new JSONArray(assetText("jvm-options.json"));
                for (int i=0; i<defaults.length(); i++)
                    if (!defaults.getString(i).startsWith("-Dcacio.managed.screensize=")) options.add(defaults.getString(i));
                options.add("-Dcacio.managed.screensize=" + WIDTH + "x" + HEIGHT);
                File jre = new File(base, "jre"), cacio = new File(base, "cacio");
                options.add("-Djava.home=" + jre);
                options.add("-Dsun.boot.library.path=" + getApplicationInfo().nativeLibraryDir);
                options.add("-Djava.library.path=" + getApplicationInfo().nativeLibraryDir);
                options.add("-Djna.boot.library.path=" + getApplicationInfo().nativeLibraryDir);
                options.add("-Djna.nosys=true");
                options.add("-Djna.nounpack=true");
                options.add("-Dorg.lwjgl.librarypath=" + getApplicationInfo().nativeLibraryDir);
                options.add("-Dorg.lwjgl.opengl.explicitInit=true");
                options.add("-Dorg.lwjgl.opengles.explicitInit=true");
                options.add("-Dorg.lwjgl.opengl.libname=libOSMesa_8.so");
                options.add("-Drunelite.rlawtpath=" + getApplicationInfo().nativeLibraryDir + "/librlawt_android.so");
                NativeBridge.environment("RUNELITE_MESA_PATH", getApplicationInfo().nativeLibraryDir + "/libOSMesa_8.so");
                NativeBridge.environment("RUNELITE_NATIVE_DIR", getApplicationInfo().nativeLibraryDir);
                NativeBridge.environment("RUNELITE_RENDERER", "gles");
                String presentation=getSharedPreferences("startup",MODE_PRIVATE).getString("glesPresentation","surface");
                NativeBridge.environment("RUNELITE_GLES_SURFACE","surface".equals(presentation)?"direct":"copy");
                NativeBridge.environment("RUNELITE_GLES_READBACK","sync".equals(presentation)?"sync":"async");
                NativeBridge.environment("RUNELITE_SHADER_ADAPT", "0");
                options.add("-Drunelite.android.hdProfile="+hdProfile);
                options.add("-Drunelite.android.hdProfileRequest=v33:"+hdProfile+":"+getSharedPreferences("startup",MODE_PRIVATE).getLong("hdProfileRevision",0));
                options.add("-Drunelite.android.renderer=" + ("gles"));
                NativeBridge.environment("MESA_SHADER_CACHE_DIR", getCacheDir().toString());
                options.add("-Dsun.awt.fontconfig=" + new File(jre, "lib/fontconfig.properties"));
                options.add("-Dsun.java2d.fontpath=" + new File(jre, "lib/fonts"));
                options.add("-Duser.home=" + home);
                options.add("-Drunelite.credentials.path=" + new File(home, ".runelite/credentials.properties"));
                options.add("-Djava.io.tmpdir=" + getCacheDir());
                options.add("-XX:ErrorFile=" + new File(home, "hs_err_pid%p.log"));
                options.add("-javaagent:" + new File(cacio, "cacio-agent.jar"));
                File[] jars = cacio.listFiles((dir, name) -> name.endsWith(".jar"));
                Arrays.sort(jars);
                options.add("-Xbootclasspath/a:" + paths(jars));
                File[] client = new File(base, "client").listFiles((dir, name) -> name.endsWith(".jar"));
                Arrays.sort(client);
                options.add("-Djava.class.path=" + paths(client));
                importEnvironment();
                message("Starting actual RuneLite client…");
                startFrames();
                int result = NativeBridge.launch(getApplicationInfo().nativeLibraryDir, jre.toString(),
                    home.toString(), log.toString(), options.toArray(new String[0]));
                if (result != 0) message("RuneLite startup failed (" + result + "). Open Logs for details.");
            } catch (Throwable error) {
                try (PrintWriter writer = new PrintWriter(new FileOutputStream(log, true))) { error.printStackTrace(writer); }
                catch (IOException ignored) { }
                message("Startup failed: " + error + ". Open Logs.");
            }
        }, "runelite-jvm").start();
    }

    private static String paths(File[] files) {
        StringBuilder result = new StringBuilder();
        for (File file : files) { if (result.length() > 0) result.append(':'); result.append(file); }
        return result.toString();
    }
    private String assetText(String name) throws IOException {
        try (InputStream in = getAssets().open(name)) { return new String(read(in), StandardCharsets.UTF_8); }
    }
    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[65536];
        int count; while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count); return out.toByteArray();
    }
    private void copyAssets(String source, File target) throws IOException {
        String[] children = getAssets().list(source);
        if (children.length > 0) {
            if (!target.isDirectory() && !target.mkdirs()) throw new IOException("Cannot create " + target);
            for (String child : children) copyAssets(source + "/" + child, new File(target, child));
        } else {
            try (InputStream in = getAssets().open(source); OutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[65536]; int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            }
        }
    }
    private void prepare() throws Exception {
        File ready = new File(base, ".ready");
        String expected = assetText("payload-id.txt").trim(), previous = "";
        if (ready.isFile()) try (InputStream in = new FileInputStream(ready)) {
            previous = new String(read(in), StandardCharsets.UTF_8).trim();
        }
        if (!expected.equals(previous)) {
            deleteRuntime(base);
            base.mkdirs();
            for (String name : new String[]{"jre", "cacio", "client"}) copyAssets(name, new File(base, name));
            configureFonts(new File(base, "jre"));
            try (OutputStream out = new FileOutputStream(ready)) { out.write(expected.getBytes(StandardCharsets.UTF_8)); }
        }
        // Android HotSpot derives JAVA_HOME from its actual library location.
        // Keep real files in the JRE layout; symlinks resolve to Android's flat
        // installed library directory and lose the module-image location.
        JSONObject layout = new JSONObject(assetText("jre/native-layout.json"));
        Iterator<String> names = layout.keys();
        while (names.hasNext()) {
            String name = names.next(); File target = new File(new File(base, "jre"), name);
            target.getParentFile().mkdirs();
            File installed = new File(getApplicationInfo().nativeLibraryDir, layout.getString(name));
            if (java.nio.file.Files.isSymbolicLink(target.toPath()))
                java.nio.file.Files.delete(target.toPath());
            if (!target.isFile() || target.length() != installed.length()) {
                java.nio.file.Files.copy(installed.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                target.setReadable(true, true);
                target.setExecutable(true, true);
            }
        }
        File settings = new File(home, ".runelite/settings.properties");
        if (!settings.exists() && !new File(home, ".runelite/profiles2").exists()) {
            settings.getParentFile().mkdirs();
            try (Writer out = new OutputStreamWriter(new FileOutputStream(settings), StandardCharsets.UTF_8)) {
                out.write("runelite.gpuplugin=false\nrunelite.discordplugin=false\nrunelite.gameSize=765x503\nrunelite.uiEnableCustomChrome=false\nrunelite.lockWindowSize=false\n");
            }
        }
    }
    private static void deleteRuntime(File file) throws IOException {
        if (java.nio.file.Files.isSymbolicLink(file.toPath())) {
            java.nio.file.Files.deleteIfExists(file.toPath()); return;
        }
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) throw new IOException("Cannot list " + file);
            for (File child : children) deleteRuntime(child);
        }
        if (!file.delete()) throw new IOException("Cannot remove old runtime " + file);
    }
    private void configureFonts(File jre) throws IOException {
        Properties fonts = new Properties(); fonts.setProperty("version", "1");
        fonts.setProperty("sequence.allfonts", "default");
        for (String family : new String[]{"serif", "sansserif", "monospaced", "dialog", "dialoginput"})
            for (String style : new String[]{"plain", "bold", "italic", "bolditalic"})
                fonts.setProperty(family + "." + style + ".default", "Noto Sans");
        fonts.setProperty("filename.Noto_Sans", new File(jre, "lib/fonts/NotoSans-Regular.ttf").toString());
        try (OutputStream out = new FileOutputStream(new File(jre, "lib/fontconfig.properties"))) {
            fonts.store(out, "Bundled open font for Android Java2D");
        }
    }
    private void importEnvironment() throws IOException {
        File file = new File(home, ".runelite/credentials.properties");
        if (!file.isFile()) return;
        Properties values = new Properties();
        try (InputStream in = new FileInputStream(file)) { values.load(in); }
        // Set only recognised launcher variables. Never print tokens to the log.
        String[][] fields = {{"JX_ACCESS_TOKEN", "accessToken"}, {"JX_REFRESH_TOKEN", "refreshToken"},
            {"JX_SESSION_ID", "sessionId"}, {"JX_CHARACTER_ID", "characterId"}, {"JX_DISPLAY_NAME", "displayName"}};
        for (String[] field : fields) {
            String value = values.getProperty(field[0], values.getProperty(field[1]));
            if (value != null) NativeBridge.environment(field[0], value);
        }
    }
    private void startFrames() {
        drawing = true;
        new Thread(() -> {
            boolean first = true, textFocused=false;long checked=0;
            while (drawing) {
                long frameStarted=System.nanoTime();
                int[] pixels = NativeBridge.pixels();
                if (pixels != null && pixels.length == WIDTH * HEIGHT) {
                    screen.frame(pixels,NativeBridge.presentation());
                    if (first) { first = false;ui.post(()->launchStatus.setVisibility(View.GONE)); message("117HD mobile • tap to click • hold for menu • drag camera • pinch zoom • Back for controls"); }
                }
                if(System.currentTimeMillis()-checked>200) {
                    checked=System.currentTimeMillis();boolean focused=NativeBridge.textInputFocused();
                    if(focused!=textFocused) {textFocused=focused;ui.post(()->setKeyboard(focused));}
                }
                long remaining=33_333_333L-(System.nanoTime()-frameStarted);
                try {
                    if(remaining>0)Thread.sleep(remaining/1_000_000L,(int)(remaining%1_000_000L));
                } catch (InterruptedException ignored) { return; }
            }
        }, "swing-render").start();
    }
    private void setKeyboard(boolean show) {
        if(isFinishing())return;
        InputMethodManager input=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        keyboardShown=show;
        if(show) {screen.requestFocus();input.restartInput(screen);input.showSoftInput(screen,InputMethodManager.SHOW_IMPLICIT);}
        else input.hideSoftInputFromWindow(screen.getWindowToken(),0);
    }
    private void sendText(CharSequence value) {
        if(value!=null) for(int i=0;i<value.length();i++)NativeBridge.input(1000,value.charAt(i),0,0,0);
    }
    private void key(int code) {
        NativeBridge.input(1005, 0, code, 1, 0); NativeBridge.input(1005, 0, code, 0, 0);
    }
    private void type() {
        EditText text = new EditText(this); text.setSingleLine(true);
        new AlertDialog.Builder(this).setTitle("Send text to focused RuneLite field").setView(text)
            .setPositiveButton("Send", (dialog, which) -> {
                for (char c : text.getText().toString().toCharArray()) NativeBridge.input(1000, c, 0, 0, 0);
            }).setNegativeButton("Cancel", null).show();
    }
    private void showLogs() {
        try {
            String value;
            try (InputStream in = new FileInputStream(log)) { value = new String(read(in), StandardCharsets.UTF_8); }
            TextView text = new TextView(this); text.setTextIsSelectable(true); text.setText(value);
            ScrollView scroll = new ScrollView(this); scroll.addView(text);
            new AlertDialog.Builder(this).setTitle("Startup log").setView(scroll)
                .setPositiveButton("Close", null).setNeutralButton("Export", (d, w) ->
                    startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain")
                        .putExtra(Intent.EXTRA_TITLE, "runelite-android-startup.log"), EXPORT)).show();
        } catch (IOException error) { message("No startup log yet."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent intent) {
        super.onActivityResult(request, result, intent);
        if (result != RESULT_OK || intent == null || intent.getData() == null) {
            if(request==IMPORT && !started) showLoginSetup();
            return;
        }
        try {
            if (request == IMPORT) {
                File credentials = new File(home, ".runelite/credentials.properties"); credentials.getParentFile().mkdirs();
                byte[] data;
                try (InputStream in = getContentResolver().openInputStream(intent.getData())) {
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream(); byte[] buffer=new byte[4096];int count;
                    while((count=in.read(buffer))!=-1) {
                        if(bytes.size()+count>65536) throw new IOException("Login file is too large");
                        bytes.write(buffer,0,count);
                    } data=bytes.toByteArray();
                }
                Properties check = new Properties(); check.load(new ByteArrayInputStream(data));
                String character=check.getProperty("characterId",check.getProperty("JX_CHARACTER_ID",""));
                String session=check.getProperty("sessionId",check.getProperty("JX_SESSION_ID",""));
                String access=check.getProperty("accessToken",check.getProperty("JX_ACCESS_TOKEN",""));
                if(character.trim().isEmpty() || (session.trim().isEmpty() && access.trim().isEmpty()))
                    throw new IOException("Choose the credentials.properties file from desktop RuneLite");
                android.util.AtomicFile storage=new android.util.AtomicFile(credentials);
                FileOutputStream out=null;
                try { out=storage.startWrite();out.write(data);storage.finishWrite(out); }
                catch(IOException failure) { if(out!=null)storage.failWrite(out);throw failure; }
                if(started)new AlertDialog.Builder(this).setTitle("Login saved").setMessage("Close and reopen to use the imported account.")
                    .setPositiveButton("Close app",(d,w)->closeClient()).setNegativeButton("Later",null).show();
            } else if (request == EXPORT) {
                try (InputStream in = new FileInputStream(log); OutputStream out = getContentResolver().openOutputStream(intent.getData())) {
                    out.write(read(in));
                }
                message("Log exported.");
            }
        } catch (Exception error) {
            // Never echo file contents or token-bearing parser errors.
            Toast.makeText(this,request==IMPORT?"Login import failed. Choose a valid desktop credentials.properties file.":"File operation failed",Toast.LENGTH_LONG).show();
        } finally { if(request==IMPORT && !started) showLoginSetup(); }
    }
    private void showControls() {
        if(controls!=null && controls.isShowing())return;
        String[] labels={"Keyboard","Graphics quality","Interface size","Enlarge sidebar","Manage login","Help & advanced","Close app"};
        controls=new AlertDialog.Builder(this).setTitle("Runelite Mobile (HD)")
            .setItems(labels,(dialog,which)-> {
                switch(which) {
                    case 0:screen.postDelayed(()->setKeyboard(true),150);break;
                    case 1:showGraphicsOptions();break;
                    case 2:gameInterface.show();break;
                    case 3:screen.enlargeSidebar();Toast.makeText(this,"Pinch to resize, use two fingers to pan. Back restores the game.",Toast.LENGTH_LONG).show();break;
                    case 4:manageLogin();break;
                    case 5:showAdvancedControls();break;
                    case 6:closeClient();break;
                }
            }).setPositiveButton("Resume",(d,w)->screen.requestFocus()).create();
        controls.show();
    }
    private void showAdvancedControls() {
        if(toolbar.getParent()!=null)((android.view.ViewGroup)toolbar.getParent()).removeView(toolbar);
        ScrollView scroll=new ScrollView(this);scroll.addView(toolbar);
        controls=new AlertDialog.Builder(this).setTitle("Help & advanced")
            .setMessage("Tap: click • hold: menu • drag scene: camera • drag item: move • pinch: zoom. Back opens the app menu.")
            .setView(scroll).setPositiveButton("Done",(d,w)->screen.requestFocus())
            .setNeutralButton("Login help",(d,w)->showLoginHelp()).create();controls.show();
    }
    @Override public void onBackPressed() {
        if(keyboardShown) {setKeyboard(false);return;}
        if(screen.interfaceZoom>1f) { screen.cancelGesture();screen.interfaceZoom=1f;screen.invalidate();return; }
        if(!started) { showLoginSetup();return; }
        showControls();
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    @Override protected void onPause() { if (screen != null) screen.cancelGesture(); super.onPause(); }
    @Override protected void onDestroy() { drawing = false; NativeBridge.surface(null);super.onDestroy(); }

    private final class SwingView extends View {
        private final ScaleGestureDetector zoom;
        private float zoomSteps, pinchSpan;
        private boolean pinchScene;
        private float interfaceZoom=1f, panY, previousFocusY;
        private final Rect source=new Rect();
        private int virtualX(float x) { return Math.max(0,Math.min(WIDTH-1,source.left+(int)((x-destination.left)*source.width()/destination.width()))); }
        private int virtualY(float y) { return Math.max(0,Math.min(HEIGHT-1,source.top+(int)((y-destination.top)*source.height()/destination.height()))); }
        private Bitmap bitmap;
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint blackPaint=new Paint();
        private final RectF destination = new RectF();
        private boolean ready;
        private int[] presentation=new int[5];
        private int surfaceWidth,surfaceHeight;
        private int pressedMask;
        private boolean sidebarDragging;
        SwingView(Context context) {
            super(context); setFocusableInTouchMode(true);
            zoom = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                    cancelGesture(); multi = true; zoomSteps = 0; previousFocusY=detector.getFocusY(); return true;
                }
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    if (destination.width() <= 0) return true;
                    if(interfaceZoom>1f) {
                        interfaceZoom=Math.max(1.5f,Math.min(3f,interfaceZoom*detector.getScaleFactor()));
                        invalidate();return true;
                    }
                    int x=virtualX(detector.getFocusX()),y=virtualY(detector.getFocusY());
                    NativeBridge.input(1003,x,y,0,0);
                    zoomSteps += Math.log(detector.getScaleFactor())/Math.log(1.08);
                    int steps=(int)zoomSteps;
                    if (steps!=0) { NativeBridge.input(1007,-steps,0,0,0); zoomSteps-=steps; }
                    return true;
                }
            });
            // The render worker updates this bitmap. Software drawing completes
            // before releasing the monitor, avoiding asynchronous texture reads.
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        synchronized void frame(int[] pixels,int[] layout) {
            presentation=layout!=null && layout.length==5?layout:new int[5];
            if (bitmap == null || bitmap.getWidth() != WIDTH || bitmap.getHeight() != HEIGHT)
                bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT); ready = true; postInvalidateOnAnimation();
        }
        @Override protected synchronized void onDraw(Canvas canvas) {
            canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
            // Insets can settle after the JVM viewport is created. Map both the
            // game Surface and touch coordinates to the current usable area.
            destination.set(0, 0, getWidth(), getHeight());
            int sw=Math.max(1,Math.round(WIDTH/interfaceZoom)),sh=Math.max(1,Math.round(HEIGHT/interfaceZoom));
            panY=Math.max(0,Math.min(HEIGHT-sh,panY));
            source.set(WIDTH-sw,(int)panY,WIDTH,(int)panY+sh);
            if(presentation[0]==1 && presentation[3]>0 && presentation[4]>0) {
                int w=presentation[3],h=presentation[4];
                if(surfaceWidth!=w || surfaceHeight!=h) {surfaceWidth=w;surfaceHeight=h;gameSurface.getHolder().setFixedSize(w,h);}
                float sx=destination.width()/source.width(),sy=destination.height()/source.height();
                int leftPx=Math.round(destination.left+(presentation[1]-source.left)*sx),topPx=Math.round(destination.top+(presentation[2]-source.top)*sy);
                int widthPx=Math.max(1,Math.round(w*sx)),heightPx=Math.max(1,Math.round(h*sy));
                FrameLayout.LayoutParams params=(FrameLayout.LayoutParams)gameSurface.getLayoutParams();
                params.gravity=Gravity.TOP|Gravity.LEFT;
                if(params.width!=widthPx || params.height!=heightPx || params.leftMargin!=leftPx || params.topMargin!=topPx) {
                    params.width=widthPx;params.height=heightPx;params.leftMargin=leftPx;params.topMargin=topPx;gameSurface.setLayoutParams(params);
                }
            }
            // Cover letterboxing even while the game Surface lies behind it.
            canvas.drawRect(0,0,getWidth(),destination.top,blackPaint);
            canvas.drawRect(0,destination.bottom,getWidth(),getHeight(),blackPaint);
            canvas.drawRect(0,destination.top,destination.left,destination.bottom,blackPaint);
            canvas.drawRect(destination.right,destination.top,getWidth(),destination.bottom,blackPaint);
            if (ready) canvas.drawBitmap(bitmap, source, destination, paint);
        }
        void enlargeSidebar() {
            interfaceZoom=ViewportSizing.sidebarZoom(HEIGHT);panY=0;invalidate();
        }
        private float downX, downY;
        private int lastX, lastY;
        private boolean committed, multi, gameDrag, longPressed, menuMoved;
        private int previousX, previousY;
        private final Runnable hold = () -> {
            if (!committed && !multi) {
                committed = true; longPressed=true;
                NativeBridge.input(1003, lastX, lastY, 0, 0);
                NativeBridge.input(1006, 4100, 1, 0, 0);
                NativeBridge.input(1006, 4100, 0, 0, 0);
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        };
        void cancelGesture() { ui.removeCallbacks(hold); releaseMouse(); committed = true; longPressed=false;pinchScene=false;pinchSpan=0;gameDrag=false; }
        private void releaseMouse() {
            if(sidebarDragging) {NativeBridge.input(1014,0,0,0,0);sidebarDragging=false;}
            if (pressedMask != 0) NativeBridge.input(1006, pressedMask, 0, 0, 0);
            pressedMask = 0;
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (!ready || destination.width() <= 0) return true;
            if(interfaceZoom>1f) zoom.onTouchEvent(event);
            int action = event.getActionMasked();
            previousX=lastX;previousY=lastY;
            lastX=virtualX(event.getX());lastY=virtualY(event.getY());
            NativeBridge.input(1003, lastX, lastY, 0, 0);
            if (action == MotionEvent.ACTION_DOWN) {
                releaseMouse(); committed = false; multi = false; longPressed=false;menuMoved=false;
                if (!destination.contains(event.getX(), event.getY())) { committed = true; return true; }
                downX = event.getX(); downY = event.getY();
                gameDrag = interfaceZoom==1f && automaticDrag && NativeBridge.gameCanvasAt(lastX, lastY);
                ui.postDelayed(hold, ViewConfiguration.getLongPressTimeout());
            } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
                ui.removeCallbacks(hold); releaseMouse(); committed = true; multi = true;
                previousFocusY=(event.getY(0)+event.getY(1))/2f;
                pinchSpan=(float)Math.hypot(event.getX(1)-event.getX(0),event.getY(1)-event.getY(0));zoomSteps=0;
                pinchScene=interfaceZoom==1f && NativeBridge.gameCanvasAt(
                    virtualX((event.getX(0)+event.getX(1))/2f),virtualY(previousFocusY));
                // Two-finger gestures zoom; suppress accidental single-finger clicks.
            } else if(action==MotionEvent.ACTION_MOVE && multi && interfaceZoom>1f && event.getPointerCount()>=2) {
                float focusY=(event.getY(0)+event.getY(1))/2f;
                panY-=(focusY-previousFocusY)*source.height()/destination.height();
                previousFocusY=focusY;invalidate();
            } else if(action==MotionEvent.ACTION_MOVE && multi && interfaceZoom==1f && event.getPointerCount()>=2) {
                float span=(float)Math.hypot(event.getX(1)-event.getX(0),event.getY(1)-event.getY(0));
                if(pinchScene && pinchSpan>0 && span>0) {
                    zoomSteps+=Math.log(span/pinchSpan)/Math.log(1.035);
                    int steps=(int)zoomSteps;
                    if(steps!=0) {
                        int focusX=virtualX((event.getX(0)+event.getX(1))/2f);
                        int focusY=virtualY((event.getY(0)+event.getY(1))/2f);
                        NativeBridge.input(1011,-steps,focusX,focusY,0);zoomSteps-=steps;
                    }
                }pinchSpan=span;
            } else if (action == MotionEvent.ACTION_MOVE && longPressed && !multi) {
                float dx=event.getX()-downX,dy=event.getY()-downY;
                int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if(dx*dx+dy*dy>slop*slop)menuMoved=true;
            } else if (action == MotionEvent.ACTION_MOVE && committed && gameDrag && !multi) {
                NativeBridge.input(1008,(reverseCamera?-1:1)*Math.round((lastX-previousX)*cameraSensitivity),Math.round((lastY-previousY)*cameraSensitivity),0,0);
            } else if (action == MotionEvent.ACTION_MOVE && committed && sidebarDragging && !multi) {
                NativeBridge.input(1013,lastX,lastY,0,0);
            } else if (action == MotionEvent.ACTION_MOVE && !committed) {
                float dx = event.getX()-downX, dy = event.getY()-downY;
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (dx*dx + dy*dy > slop*slop) {
                    ui.removeCallbacks(hold); committed = true;
                    if(gameDrag) {
                        // Start relative rotation here; do not rewind a stale emulated mouse anchor.
                        NativeBridge.input(1008,(reverseCamera?-1:1)*Math.round((lastX-previousX)*cameraSensitivity),Math.round((lastY-previousY)*cameraSensitivity),0,0);
                    } else {
                        sidebarDragging=true;
                        NativeBridge.input(1012,virtualX(downX),virtualY(downY),0,0);
                        NativeBridge.input(1013,lastX,lastY,0,0);
                    }
                }
            } else if (action == MotionEvent.ACTION_POINTER_UP) {
                pinchScene=false;pinchSpan=0;releaseMouse();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                ui.removeCallbacks(hold);
                if(action==MotionEvent.ACTION_UP && longPressed && menuMoved && !multi)
                    NativeBridge.input(1009,0,0,0,0);
                if (action == MotionEvent.ACTION_UP && !committed) {
                    NativeBridge.input(1006, mouseMask, 1, 0, 0);
                    NativeBridge.input(1006, mouseMask, 0, 0, 0);
                    ui.postDelayed(()-> {if(NativeBridge.textInputFocused())setKeyboard(true);},150);

                }
                releaseMouse(); committed = true;
            }
            return true;
        }
        @Override public boolean onCheckIsTextEditor() {return true;}
        @Override public InputConnection onCreateInputConnection(EditorInfo info) {
            info.inputType=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
            info.imeOptions=EditorInfo.IME_ACTION_DONE|EditorInfo.IME_FLAG_NO_EXTRACT_UI|EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
            return new BaseInputConnection(this,false) {
                private String composing="";
                @Override public boolean setComposingText(CharSequence value,int cursor) {composing=value.toString();return true;}
                @Override public boolean commitText(CharSequence value,int cursor) {composing="";sendText(value);return true;}
                @Override public boolean finishComposingText() {sendText(composing);composing="";return true;}
                @Override public CharSequence getTextBeforeCursor(int length,int flags) {return composing.substring(Math.max(0,composing.length()-length));}
                @Override public CharSequence getTextAfterCursor(int length,int flags) {return "";}
                @Override public boolean deleteSurroundingText(int before,int after) {
                    if(!composing.isEmpty()) composing=composing.substring(0,Math.max(0,composing.length()-before));
                    else for(int i=0;i<Math.min(before,256);i++)key(8);
                    for(int i=0;i<Math.min(after,256);i++)key(127);
                    return true;
                }
                @Override public boolean performEditorAction(int action) {finishComposingText();key(10);setKeyboard(false);return true;}
                @Override public boolean sendKeyEvent(KeyEvent event) {
                    return event.getAction()!=KeyEvent.ACTION_DOWN || SwingView.this.onKeyDown(event.getKeyCode(),event);
                }
            };
        }
        @Override public boolean onKeyPreIme(int code,KeyEvent event) {
            if(code==KeyEvent.KEYCODE_BACK && keyboardShown) {
                if(event.getAction()==KeyEvent.ACTION_UP)setKeyboard(false);
                return true;
            }return super.onKeyPreIme(code,event);
        }
        @Override public boolean onKeyDown(int code, KeyEvent event) {
            if (code == KeyEvent.KEYCODE_DEL) { key(8); return true; }
            if (code == KeyEvent.KEYCODE_ENTER) { key(10); return true; }
            int unicode = event.getUnicodeChar();
            if (unicode != 0) { NativeBridge.input(1000, unicode, 0, 0, 0); return true; }
            return super.onKeyDown(code, event);
        }
    }
}
