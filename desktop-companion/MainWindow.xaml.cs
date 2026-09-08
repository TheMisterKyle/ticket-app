using System.Windows;
using System.Windows.Controls;
using Forms = System.Windows.Forms;

namespace TicketToss.Companion;

public partial class MainWindow : Window
{
    private readonly Forms.Screen[] screens;
    private readonly CompanionServer server;
    private bool overlayBusy;

    public MainWindow()
    {
        InitializeComponent();
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
        if (overlayBusy) return;
        overlayBusy = true;
        try
        {
            var screen = screens[Math.Max(0, DisplayPicker.SelectedIndex)];
            var overlay = new OverlayWindow(screen, PlaySoundCheck.IsChecked == true);
            await overlay.PlayAsync(outcome);
        }
        finally
        {
            overlayBusy = false;
        }
    }

    public void StopServer() => server.Dispose();
}
