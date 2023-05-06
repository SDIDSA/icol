package icol;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.prefs.Preferences;

import static java.lang.System.setErr;
import static java.util.prefs.Preferences.systemRoot;

public class AdminCheck
{
    public static final boolean IS_RUNNING_AS_ADMINISTRATOR;

    static
    {
        IS_RUNNING_AS_ADMINISTRATOR = isRunningAsAdministrator();
    }

    private static boolean isRunningAsAdministrator()
    {
        Preferences preferences = systemRoot();

        synchronized (System.err)
        {
            setErr(new PrintStream(new OutputStream()
            {
                @Override
                public void write(int b)
                {
                }
            }));

            try
            {
                preferences.put("foo", "bar"); // SecurityException on Windows
                preferences.remove("foo");
                preferences.flush(); // BackingStoreException on Linux
                return true;
            } catch (Exception exception)
            {
                return false;
            } finally
            {
                setErr(System.err);
            }
        }
    }
}