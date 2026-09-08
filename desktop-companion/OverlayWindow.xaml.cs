using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;
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
        var filename = green ? "ticket_green.png" : "ticket_red.png";
        var path = System.IO.Path.Combine(AppContext.BaseDirectory, "Assets", filename);
        TicketImage.Source = new BitmapImage(new Uri(path));

        if (outcome is Outcome.GreenWin or Outcome.RedWin)
        {
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

        var duration = TimeSpan.FromMilliseconds(1900);
        var x = new DoubleAnimationUsingKeyFrames { Duration = duration };
        var y = new DoubleAnimationUsingKeyFrames { Duration = duration };
        var opacity = new DoubleAnimationUsingKeyFrames { Duration = duration };
        if (outcome == Outcome.GreenMiss)
        {
            x.KeyFrames.Add(new LinearDoubleKeyFrame(-screenBounds.Width * .65, KeyTime.FromPercent(0)));
            x.KeyFrames.Add(new SplineDoubleKeyFrame(0, KeyTime.FromPercent(.38), new KeySpline(.2, .8, .3, 1)));
            x.KeyFrames.Add(new SplineDoubleKeyFrame(screenBounds.Width * .75, KeyTime.FromPercent(1), new KeySpline(.7, 0, 1, .4)));
            y.KeyFrames.Add(new LinearDoubleKeyFrame(35, KeyTime.FromPercent(0)));
            y.KeyFrames.Add(new LinearDoubleKeyFrame(-25, KeyTime.FromPercent(1)));
            TicketRotation.Angle = -8;
            TicketRotation.BeginAnimation(System.Windows.Media.RotateTransform.AngleProperty,
                new DoubleAnimation(-8, 13, duration));
        }
        else
        {
            x.KeyFrames.Add(new LinearDoubleKeyFrame(0, KeyTime.FromPercent(0)));
            x.KeyFrames.Add(new LinearDoubleKeyFrame(0, KeyTime.FromPercent(.38)));
            x.KeyFrames.Add(new SplineDoubleKeyFrame(screenBounds.Width * .62, KeyTime.FromPercent(1), new KeySpline(.5, 0, 1, .4)));
            y.KeyFrames.Add(new LinearDoubleKeyFrame(0, KeyTime.FromPercent(0)));
            y.KeyFrames.Add(new LinearDoubleKeyFrame(0, KeyTime.FromPercent(.38)));
            y.KeyFrames.Add(new SplineDoubleKeyFrame(-screenBounds.Height * .55, KeyTime.FromPercent(1), new KeySpline(.5, 0, 1, .4)));
            TicketRotation.BeginAnimation(System.Windows.Media.RotateTransform.AngleProperty,
                new DoubleAnimation(0, 24, duration));
        }
        opacity.KeyFrames.Add(new LinearDoubleKeyFrame(0, KeyTime.FromPercent(0)));
        opacity.KeyFrames.Add(new LinearDoubleKeyFrame(1, KeyTime.FromPercent(.08)));
        opacity.KeyFrames.Add(new LinearDoubleKeyFrame(1, KeyTime.FromPercent(.70)));
        opacity.KeyFrames.Add(new LinearDoubleKeyFrame(0, KeyTime.FromPercent(1)));
        TicketTranslation.BeginAnimation(System.Windows.Media.TranslateTransform.XProperty, x);
        TicketTranslation.BeginAnimation(System.Windows.Media.TranslateTransform.YProperty, y);
        TicketImage.BeginAnimation(OpacityProperty, opacity);
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
