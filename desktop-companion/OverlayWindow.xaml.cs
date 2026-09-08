using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
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
    private DispatcherTimer? animationTimer;

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
        animationTimer?.Stop();
        Close();
    }

    private void PlayAnimation(Outcome outcome)
    {
        var filename = outcome switch
        {
            Outcome.GreenWin => "green_win.gif",
            Outcome.RedWin => "red_win.gif",
            Outcome.GreenMiss => "green_miss.gif",
            _ => "red_miss.gif",
        };
        var path = System.IO.Path.Combine(AppContext.BaseDirectory, "Assets", filename);
        using var stream = System.IO.File.OpenRead(path);
        var decoder = new GifBitmapDecoder(stream, BitmapCreateOptions.PreservePixelFormat,
            BitmapCacheOption.OnLoad);
        var frames = decoder.Frames.ToArray();
        if (frames.Length == 0) return;

        var frameIndex = 0;
        AnimationImage.Source = frames[0];
        AnimationImage.BeginAnimation(OpacityProperty,
            new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(90)));
        animationTimer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(1000.0 / 24.0) };
        animationTimer.Tick += (_, _) =>
        {
            frameIndex++;
            if (frameIndex >= frames.Length)
            {
                animationTimer.Stop();
                return;
            }
            AnimationImage.Source = frames[frameIndex];
        };
        animationTimer.Start();
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
