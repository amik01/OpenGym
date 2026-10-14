package ch.duartesantos.opengym;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Native Android smoke test with a fresh, synthetic profile and bundled offline assets. */
@RunWith(AndroidJUnit4.class)
public class AppSmokeTest {
    private String evaluate(ActivityScenario<MainActivity> activity, String script) throws Exception {
        ArrayBlockingQueue<String> result = new ArrayBlockingQueue<>(1);
        activity.onActivity(a -> a.getBridge().getWebView().evaluateJavascript(script, result::offer));
        String value = result.poll(10, TimeUnit.SECONDS);
        assertNotNull("WebView did not answer", value);
        return value;
    }

    private void awaitTrue(ActivityScenario<MainActivity> activity, String condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
        while (System.nanoTime() < deadline) {
            if ("true".equals(evaluate(activity, condition))) return;
            Thread.sleep(100);
        }
        fail("Timed out waiting for: " + condition);
    }

    @Test
    public void nativeAppLoadsChoosesLocalModeAndSurvivesRecreation() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("ch.duartesantos.opengym.test", context.getPackageName());
        try (ActivityScenario<MainActivity> activity = ActivityScenario.launch(MainActivity.class)) {
            awaitTrue(activity, "document.querySelector('meta[name=opengym-flavor]')?.content === 'mobile'"
                    + " && window.Capacitor?.getPlatform() === 'android'"
                    + " && document.body.innerText.includes('How do you want to use openGym?')");
            assertEquals("true", evaluate(activity,
                    "(() => { const button = [...document.querySelectorAll('button')]"
                    + ".find(b => b.innerText.includes('Use on this device'));"
                    + " if (!button) return false; button.click(); return true; })()"));
            awaitTrue(activity, "!!document.querySelector('#tabbar') && location.hash.includes('/home')");

            File connection = new File(context.getFilesDir(), "opengym-remote.json");
            assertTrue("Local choice must reach Android private storage", connection.isFile());
            JSONObject saved = new JSONObject(new String(Files.readAllBytes(connection.toPath()), StandardCharsets.UTF_8));
            assertEquals("local", saved.getString("mode"));

            evaluate(activity, "window.__smokeMedia = null; fetch('/exercise-media/still/0001.webp')"
                    + ".then(r => r.blob().then(b => { window.__smokeMedia = r.ok && b.size > 0; }))"
                    + ".catch(() => { window.__smokeMedia = false; });");
            awaitTrue(activity, "window.__smokeMedia === true");

            evaluate(activity, "document.querySelectorAll('#tabbar button')[4].click()");
            awaitTrue(activity, "location.hash.includes('/library')"
                    + " && document.querySelector('#app .lib-hdr h1')?.textContent === 'Exercises'"
                    + " && document.querySelectorAll('#app .list .item:not(.ex-new)').length > 0");
            activity.recreate();
            awaitTrue(activity, "!!document.querySelector('#tabbar')"
                    + " && !document.body.innerText.includes('How do you want to use openGym?')");

            Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull("Android screenshot unavailable", screenshot);
            File folder = context.getExternalFilesDir("screenshots");
            assertNotNull(folder);
            assertTrue(folder.isDirectory() || folder.mkdirs());
            try (FileOutputStream output = new FileOutputStream(new File(folder, "android-local.png"))) {
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
            } finally {
                screenshot.recycle();
            }
        }
    }
}
