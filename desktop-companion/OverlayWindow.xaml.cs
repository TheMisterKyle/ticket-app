using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
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
        if (playSound) PlayAsset(outcome switch
        {
            Outcome.GreenWin => "green_win.wav",
            Outcome.RedWin => "red_win.wav",
            Outcome.GreenMiss => "green_miss.wav",
            _ => "red_miss.wav",
        });
        PlayAnimation(outcome);
        await Task.Delay(3000);
        Close();
    }

    private void PlayAnimation(Outcome outcome)
    {
        var green = outcome is Outcome.GreenWin or Outcome.GreenMiss;
        var win = outcome is Outcome.GreenWin or Outcome.RedWin;
        var preferredFilename = green ? "ticket_green_nope.png" : "ticket_red_nope.png";
        var filename = win || !System.IO.File.Exists(
            System.IO.Path.Combine(AppContext.BaseDirectory, "Assets", preferredFilename))
            ? (green ? "ticket_green.png" : "ticket_red.png")
            : preferredFilename;
        var path = System.IO.Path.Combine(AppContext.BaseDirectory, "Assets", filename);
        TicketImage.Source = new BitmapImage(new Uri(path));

        if (outcome is Outcome.GreenWin or Outcome.RedWin)
        {
            AddSparkles(green);
            TicketScale.ScaleX = TicketScale.ScaleY = outcome == Outcome.GreenWin ? .48 : 1.35;
            TicketRotation.Angle = outcome == Outcome.GreenWin ? -12 : 9;
            TicketTranslation.Y = outcome == Outcome.RedWin ? -screenBounds.Height * .55 : 0;
            TicketImage.BeginAnimation(OpacityProperty,
                new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(120)));
            var settle = new BackEase { Amplitude = .45, EasingMode = EasingMode.EaseOut };
            TicketScale.BeginAnimation(System.Windows.Media.ScaleTransform.ScaleXProperty,
                new DoubleAnimation(TicketScale.ScaleX, 1, TimeSpan.FromMilliseconds(650)) { EasingFunction = settle });
            TicketScale.BeginAnimation(System.Windows.Media.ScaleTransform.ScaleYProperty,
                new DoubleAnimation(TicketScale.ScaleY, 1, TimeSpan.FromMilliseconds(650)) { EasingFunction = settle });
            TicketRotation.BeginAnimation(System.Windows.Media.RotateTransform.AngleProperty,
                new DoubleAnimation(TicketRotation.Angle, 0, TimeSpan.FromMilliseconds(650)) { EasingFunction = settle });
            TicketTranslation.BeginAnimation(System.Windows.Media.TranslateTransform.YProperty,
                new DoubleAnimation(TicketTranslation.Y, 0, TimeSpan.FromMilliseconds(650)) { EasingFunction = settle });
            return;
        }

        PlayRippedTicket(TicketImage.Source, outcome == Outcome.GreenMiss);
    }

    private void AddSparkles(bool green)
    {
        EffectsCanvas.Children.Clear();
        var colour = green ? Color.FromRgb(255, 218, 91) : Color.FromRgb(255, 239, 220);
        var positions = new (double X, double Y, double Size, int Delay)[]
        {
            (105, 118, 34, 90), (205, 72, 22, 310), (348, 105, 27, 170),
            (590, 88, 31, 390), (735, 130, 24, 220), (830, 205, 36, 520),
            (118, 390, 28, 450), (236, 505, 35, 260), (375, 462, 21, 610),
            (585, 490, 29, 120), (742, 455, 38, 350), (842, 350, 23, 680),
        };
        foreach (var (x, y, size, delay) in positions)
        {
            var sparkle = new TextBlock
            {
                Text = "✦",
                FontSize = size,
                Foreground = new SolidColorBrush(colour),
                Opacity = 0,
                RenderTransformOrigin = new Point(.5, .5),
                RenderTransform = new ScaleTransform(.25, .25),
            };
            Canvas.SetLeft(sparkle, x);
            Canvas.SetTop(sparkle, y);
            EffectsCanvas.Children.Add(sparkle);
            var begin = TimeSpan.FromMilliseconds(delay);
            var pulse = new DoubleAnimationUsingKeyFrames { BeginTime = begin, Duration = TimeSpan.FromMilliseconds(900) };
            pulse.KeyFrames.Add(new LinearDoubleKeyFrame(0, KeyTime.FromPercent(0)));
            pulse.KeyFrames.Add(new SplineDoubleKeyFrame(1, KeyTime.FromPercent(.28), new KeySpline(.2, .8, .3, 1)));
            pulse.KeyFrames.Add(new LinearDoubleKeyFrame(.85, KeyTime.FromPercent(.68)));
            pulse.KeyFrames.Add(new LinearDoubleKeyFrame(0, KeyTime.FromPercent(1)));
            sparkle.BeginAnimation(OpacityProperty, pulse);
            var scale = (ScaleTransform)sparkle.RenderTransform;
            scale.BeginAnimation(ScaleTransform.ScaleXProperty,
                new DoubleAnimation(.25, 1.35, TimeSpan.FromMilliseconds(700)) { BeginTime = begin });
            scale.BeginAnimation(ScaleTransform.ScaleYProperty,
                new DoubleAnimation(.25, 1.35, TimeSpan.FromMilliseconds(700)) { BeginTime = begin });
        }
    }

    private void PlayRippedTicket(ImageSource source, bool greenMiss)
    {
        TicketImage.Visibility = Visibility.Collapsed;
        RippedTicket.Visibility = Visibility.Visible;
        RipLeft.Source = RipRight.Source = source;
        RippedTicket.Opacity = 0;
        RippedTicket.BeginAnimation(OpacityProperty,
            new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(130)));

        var pause = TimeSpan.FromMilliseconds(480);
        var tearTime = TimeSpan.FromMilliseconds(1250);
        var ease = new QuadraticEase { EasingMode = EasingMode.EaseIn };
        var direction = greenMiss ? 1 : -1;
        RipLeftRotation.BeginAnimation(RotateTransform.AngleProperty,
            new DoubleAnimation(0, -24 * direction, tearTime) { BeginTime = pause, EasingFunction = ease });
        RipRightRotation.BeginAnimation(RotateTransform.AngleProperty,
            new DoubleAnimation(0, 27 * direction, tearTime) { BeginTime = pause, EasingFunction = ease });
        RipLeftTranslation.BeginAnimation(TranslateTransform.XProperty,
            new DoubleAnimation(0, -360, tearTime) { BeginTime = pause, EasingFunction = ease });
        RipRightTranslation.BeginAnimation(TranslateTransform.XProperty,
            new DoubleAnimation(0, 360, tearTime) { BeginTime = pause, EasingFunction = ease });
        RipLeftTranslation.BeginAnimation(TranslateTransform.YProperty,
            new DoubleAnimation(0, 230, tearTime) { BeginTime = pause, EasingFunction = ease });
        RipRightTranslation.BeginAnimation(TranslateTransform.YProperty,
            new DoubleAnimation(0, 250, tearTime) { BeginTime = pause, EasingFunction = ease });
        RippedTicket.BeginAnimation(OpacityProperty,
            new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(700))
                { BeginTime = TimeSpan.FromMilliseconds(1050) });
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
