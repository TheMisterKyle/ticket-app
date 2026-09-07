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
    private readonly bool playSound;
    private readonly MediaPlayer player = new();

    public OverlayWindow(Forms.Screen screen, bool playSound)
    {
        InitializeComponent();
        this.playSound = playSound;
        Left = screen.Bounds.Left;
        Top = screen.Bounds.Top;
        Width = screen.Bounds.Width;
        Height = screen.Bounds.Height;
        ResultArt.SetValue(Canvas.LeftProperty, (Width - ResultArt.Width) / 2);
        ResultArt.SetValue(Canvas.TopProperty, (Height - ResultArt.Height) / 2);
        SourceInitialized += (_, _) =>
        {
            var handle = new WindowInteropHelper(this).Handle;
            SetWindowLong(handle, GwlExStyle,
                GetWindowLong(handle, GwlExStyle) | WsExTransparent | WsExNoActivate);
        };
    }

    public async Task PlayAsync(Outcome outcome)
    {
        Show();
        if (playSound) PlayAsset("drumroll.mp3");
        await Task.Delay(950);
        Configure(outcome);
        if (playSound) PlayAsset(outcome switch
        {
            Outcome.GreenWin => "green_win.mp3",
            Outcome.RedWin => "red_win.mp3",
            Outcome.GreenMiss => "green_miss.mp3",
            _ => "red_miss.mp3",
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
        var ink = (Color)ColorConverter.ConvertFromString(green ? "#123B29" : "#481D22");
        Ticket.BorderBrush = new SolidColorBrush(ink);
        Eyebrow.Foreground = Headline.Foreground = new SolidColorBrush(ink);
        MissIcon.Text = outcome == Outcome.GreenMiss ? "☹" : "💨";
        MissIcon.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString(green ? "#37C977" : "#F5F0E6"));
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
        player.Open(new Uri(Path.Combine(AppContext.BaseDirectory, "Assets", name)));
        player.Volume = 1;
        player.Play();
    }

    [DllImport("user32.dll")] private static extern int GetWindowLong(IntPtr hWnd, int index);
    [DllImport("user32.dll")] private static extern int SetWindowLong(IntPtr hWnd, int index, int value);
}
