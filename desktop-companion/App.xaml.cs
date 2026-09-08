using System.Drawing;
using System.Windows;
using Forms = System.Windows.Forms;

namespace TicketToss.Companion;

public partial class App : System.Windows.Application
{
    private Forms.NotifyIcon? trayIcon;
    private MainWindow? controls;

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        controls = new MainWindow();
        controls.Closing += (_, args) => { args.Cancel = true; controls.Hide(); };

        var menu = new Forms.ContextMenuStrip();
        menu.Items.Add("Open controls", null, (_, _) => ShowControls());
        menu.Items.Add("Exit", null, (_, _) => Shutdown());
        trayIcon = new Forms.NotifyIcon
        {
            Icon = SystemIcons.Application,
            Text = "Ticket Toss Projector",
            Visible = true,
            ContextMenuStrip = menu,
        };
        trayIcon.DoubleClick += (_, _) => ShowControls();
        ShowControls();
    }

    private void ShowControls()
    {
        controls!.Show();
        controls.Activate();
    }

    protected override void OnExit(ExitEventArgs e)
    {
        controls?.StopServer();
        if (trayIcon is not null) trayIcon.Visible = false;
        trayIcon?.Dispose();
        base.OnExit(e);
    }
}
