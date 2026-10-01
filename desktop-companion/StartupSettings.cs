using System.IO;
using System.Text.Json;
using Microsoft.Win32;

namespace TicketToss.Companion;

public sealed class StartupSettings
{
    private const string RunKey = @"Software\Microsoft\Windows\CurrentVersion\Run";
    private const string RunName = "TicketTossCompanion";
    private readonly string settingsPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "TicketToss", "startup-settings.json");

    public bool StartWithWindows { get; private set; }
    public bool StartInTray { get; private set; }

    public StartupSettings()
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunKey);
            StartWithWindows = key?.GetValue(RunName) is string value && !string.IsNullOrWhiteSpace(value);
        }
        catch (Exception error) when (error is UnauthorizedAccessException or System.Security.SecurityException or IOException) { }
        try
        {
            if (File.Exists(settingsPath))
                StartInTray = JsonSerializer.Deserialize<TrayPreference>(File.ReadAllText(settingsPath))?.StartInTray == true;
        }
        catch (Exception error) when (error is IOException or UnauthorizedAccessException or JsonException) { }
    }

    public void SetStartWithWindows(bool enabled)
    {
        using var key = Registry.CurrentUser.CreateSubKey(RunKey);
        if (enabled)
        {
            var executable = Environment.ProcessPath ?? throw new IOException("Cannot locate the companion executable.");
            if (!executable.EndsWith("TicketToss.Companion.exe", StringComparison.OrdinalIgnoreCase))
                throw new IOException("Install and run the packaged companion before enabling Windows startup.");
            key.SetValue(RunName, "\"" + executable + "\"", RegistryValueKind.String);
        }
        else key.DeleteValue(RunName, throwOnMissingValue: false);
        StartWithWindows = enabled;
    }

    public void SetStartInTray(bool enabled)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(settingsPath)!);
        var temporary = settingsPath + ".tmp";
        File.WriteAllText(temporary, JsonSerializer.Serialize(new TrayPreference { StartInTray = enabled }));
        File.Move(temporary, settingsPath, overwrite: true);
        StartInTray = enabled;
    }

    public sealed class TrayPreference { public bool StartInTray { get; set; } }
}
