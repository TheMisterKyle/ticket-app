using System.Windows;
using System.Windows.Controls;
using Forms = System.Windows.Forms;

namespace TicketToss.Companion;

public partial class MainWindow : Window
{
    private readonly Forms.Screen[] screens;
    private readonly CompanionServer server;
    private bool overlayBusy;
    private readonly Queue<Outcome> pendingOutcomes = new();

    private readonly StartupSettings startupSettings;
    private bool startupSettingsReady;

    public MainWindow(StartupSettings settings)
    {
        startupSettings = settings;
        InitializeComponent();
        StartWithWindowsCheck.IsChecked = settings.StartWithWindows;
        StartInTrayCheck.IsChecked = settings.StartInTray;
        startupSettingsReady = true;
        screens = Forms.Screen.AllScreens;
        foreach (var screen in screens)
        {
            var role = screen.Primary ? "Primary" : "Display";
            DisplayPicker.Items.Add($"{role}: {screen.DeviceName} ({screen.Bounds.Width}×{screen.Bounds.Height})");
        }
        DisplayPicker.SelectedIndex = screens.Length > 1 ? 1 : 0;

        server = new CompanionServer();
        ConnectionAddress.Text = $"Address: {server.LocalAddress}:{CompanionServer.Port}";
        PairingCode.Text = $"Pairing code: {server.PairingCode}";
        server.OutcomeReceived += OnOutcomeReceived;
        server.Start();
    }

    private async void Test_Click(object sender, RoutedEventArgs e)
    {
        if (sender is not System.Windows.Controls.Button button || button.Tag is not string tag) return;
        var outcome = Enum.Parse<Outcome>(tag);
        await ShowOutcomeAsync(outcome);
    }

    private void OnOutcomeReceived(Outcome outcome)
    {
        Dispatcher.InvokeAsync(async () =>
        {
            ConnectionStatus.Text = $"Connected — received {outcome}";
            ConnectionStatus.Foreground = new System.Windows.Media.SolidColorBrush(
                System.Windows.Media.Color.FromRgb(55, 201, 119));
            await ShowOutcomeAsync(outcome);
        });
    }

    private async Task ShowOutcomeAsync(Outcome outcome)
    {
        pendingOutcomes.Enqueue(outcome);
        if (overlayBusy) return;
        overlayBusy = true;
        try
        {
            while (pendingOutcomes.TryDequeue(out var next)) {
                var screen = screens[Math.Max(0, DisplayPicker.SelectedIndex)];
                var overlay = new OverlayWindow(screen, PlaySoundCheck.IsChecked == true);
                await overlay.PlayAsync(next);
            }
        }
        finally
        {
            overlayBusy = false;
        }
    }

    private void StartupSetting_Changed(object sender, RoutedEventArgs e)
    {
        if (!startupSettingsReady) return;
        try
        {
            if (sender == StartWithWindowsCheck)
                startupSettings.SetStartWithWindows(StartWithWindowsCheck.IsChecked == true);
            else if (sender == StartInTrayCheck)
                startupSettings.SetStartInTray(StartInTrayCheck.IsChecked == true);
        }
        catch (Exception error) when (error is System.IO.IOException or UnauthorizedAccessException or System.Security.SecurityException)
        {
            startupSettingsReady = false;
            StartWithWindowsCheck.IsChecked = startupSettings.StartWithWindows;
            StartInTrayCheck.IsChecked = startupSettings.StartInTray;
            startupSettingsReady = true;
            System.Windows.MessageBox.Show(this, "Could not save the startup setting. " + error.Message,
                "Ticket Toss", MessageBoxButton.OK, MessageBoxImage.Warning);
        }
    }

    public void StopServer() => server.Dispose();
}
