using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using Forms = System.Windows.Forms;

namespace TicketToss.Companion;

public partial class OverlayWindow : Window
{
    private const int GwlExStyle = -20;
    private const int WsExTransparent = 0x20;
    private const int WsExNoActivate = 0x08000000;
    private const uint SwpNoActivate = 0x0010;
    private const uint SwpShowWindow = 0x0040;
    private readonly bool playSound;
    private readonly System.Drawing.Rectangle screenBounds;
    private readonly System.Media.SoundPlayer player = new();

    public OverlayWindow(Forms.Screen screen, bool playSound)
    {
        InitializeComponent();
        this.playSound = playSound;
        screenBounds = screen.Bounds;
        SourceInitialized += (_, _) =>
        {
            var handle = new WindowInteropHelper(this).Handle;
            SetWindowLong(handle, GwlExStyle,
                GetWindowLong(handle, GwlExStyle) | WsExTransparent | WsExNoActivate);
            SetWindowPos(
                handle,
                new IntPtr(-1),
                screenBounds.Left,
                screenBounds.Top,
                screenBounds.Width,
                screenBounds.Height,
                SwpNoActivate | SwpShowWindow);
        };
    }

    public async Task PlayAsync(Outcome outcome)
    {
        Show();
        if (playSound) PlayAsset("drumroll.wav");
        await Task.Delay(950);
        Configure(outcome);
        if (playSound) PlayAsset(outcome switch
        {
            Outcome.GreenWin => "green_win.wav",
            Outcome.RedWin => "red_win.wav",
            Outcome.GreenMiss => "green_miss.wav",
            _ => "red_miss.wav",
        });
        Animate(outcome);
        await Task.Delay(3000);
        Close();
    }

    private void Configure(Outcome outcome)
    {
        var green = outcome is Outcome.GreenWin or Outcome.GreenMiss;
        Ticket.Visibility = outcome is Outcome.GreenWin or Outcome.RedWin ? Visibility.Visible : Visibility.Collapsed;
        MissIcon.Visibility = Ticket.Visibility == Visibility.Visible ? Visibility.Collapsed : Visibility.Visible;
        Headline.Text = green ? "GREEN TICKET!" : "RED TICKET!";
        var ink = (System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString(
            green ? "#123B29" : "#481D22");
        Ticket.BorderBrush = new SolidColorBrush(ink);
        Eyebrow.Foreground = Headline.Foreground = new SolidColorBrush(ink);
        MissIcon.Text = outcome == Outcome.GreenMiss ? "☹" : "💨";
        MissIcon.Foreground = new SolidColorBrush(
            (System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString(
                green ? "#37C977" : "#F5F0E6"));
    }

    private void Animate(Outcome outcome)
    {
        ResultArt.BeginAnimation(OpacityProperty, new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(130)));
        var easing = new BackEase { Amplitude = .55, EasingMode = EasingMode.EaseOut };
        if (outcome == Outcome.GreenWin)
        {
            Translation.X = -Width;
            Rotation.Angle = -14;
            Translation.BeginAnimation(System.Windows.Media.TranslateTransform.XProperty,
                new DoubleAnimation(-Width, 0, TimeSpan.FromMilliseconds(620)) { EasingFunction = easing });
            Rotation.BeginAnimation(System.Windows.Media.RotateTransform.AngleProperty,
                new DoubleAnimation(-14, 0, TimeSpan.FromMilliseconds(620)) { EasingFunction = easing });
        }
        else if (outcome == Outcome.RedWin)
        {
            Translation.Y = -Height;
            Scale.ScaleX = Scale.ScaleY = 1.25;
            Translation.BeginAnimation(System.Windows.Media.TranslateTransform.YProperty,
                new DoubleAnimation(-Height, 0, TimeSpan.FromMilliseconds(430)) { EasingFunction = easing });
            Scale.BeginAnimation(System.Windows.Media.ScaleTransform.ScaleXProperty,
                new DoubleAnimation(1.25, 1, TimeSpan.FromMilliseconds(520)) { EasingFunction = easing });
            Scale.BeginAnimation(System.Windows.Media.ScaleTransform.ScaleYProperty,
                new DoubleAnimation(1.25, 1, TimeSpan.FromMilliseconds(520)) { EasingFunction = easing });
        }
        else
        {
            Scale.ScaleX = Scale.ScaleY = .55;
            Scale.BeginAnimation(System.Windows.Media.ScaleTransform.ScaleXProperty,
                new DoubleAnimation(.55, 1, TimeSpan.FromMilliseconds(520)) { EasingFunction = easing });
            Scale.BeginAnimation(System.Windows.Media.ScaleTransform.ScaleYProperty,
                new DoubleAnimation(.55, 1, TimeSpan.FromMilliseconds(520)) { EasingFunction = easing });
            Translation.BeginAnimation(System.Windows.Media.TranslateTransform.YProperty,
                new DoubleAnimation(outcome == Outcome.GreenMiss ? -25 : 25, 0, TimeSpan.FromMilliseconds(600)) { EasingFunction = easing });
        }
    }

    private void PlayAsset(string name)
    {
        player.SoundLocation = System.IO.Path.Combine(AppContext.BaseDirectory, "Assets", name);
        player.Load();
        player.Play();
    }

    [DllImport("user32.dll")] private static extern int GetWindowLong(IntPtr hWnd, int index);
    [DllImport("user32.dll")] private static extern int SetWindowLong(IntPtr hWnd, int index, int value);
    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool SetWindowPos(
        IntPtr hWnd,
        IntPtr hWndInsertAfter,
        int x,
        int y,
        int width,
        int height,
        uint flags);
}
