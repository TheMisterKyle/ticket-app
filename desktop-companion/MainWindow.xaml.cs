using System.Windows;
using System.Windows.Controls;
using Forms = System.Windows.Forms;

namespace TicketToss.Companion;

public partial class MainWindow : Window
{
    private readonly Forms.Screen[] screens;

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
    }

    private async void Test_Click(object sender, RoutedEventArgs e)
    {
        if (sender is not System.Windows.Controls.Button button || button.Tag is not string tag) return;
        var outcome = Enum.Parse<Outcome>(tag);
        var screen = screens[Math.Max(0, DisplayPicker.SelectedIndex)];
        var overlay = new OverlayWindow(screen, PlaySoundCheck.IsChecked == true);
        await overlay.PlayAsync(outcome);
    }
}
