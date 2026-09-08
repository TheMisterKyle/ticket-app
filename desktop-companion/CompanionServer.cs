using System.IO;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text;

namespace TicketToss.Companion;

public sealed class CompanionServer : IDisposable
{
    public const int Port = 45832;
    private readonly TcpListener listener = new(IPAddress.Any, Port);
    private readonly CancellationTokenSource cancellation = new();

    public string PairingCode { get; } = LoadOrCreatePairingCode();
    public string LocalAddress { get; } = FindLocalAddress();
    public event Action<Outcome>? OutcomeReceived;

    public void Start()
    {
        listener.Start();
        _ = AcceptLoopAsync();
    }

    private async Task AcceptLoopAsync()
    {
        while (!cancellation.IsCancellationRequested)
        {
            try
            {
                var client = await listener.AcceptTcpClientAsync(cancellation.Token);
                _ = HandleClientAsync(client);
            }
            catch (OperationCanceledException) { }
            catch (ObjectDisposedException) { }
            catch (SocketException) when (cancellation.IsCancellationRequested) { }
        }
    }

    private async Task HandleClientAsync(TcpClient client)
    {
        using (client)
        {
            client.ReceiveTimeout = 2500;
            client.SendTimeout = 2500;
            using var reader = new StreamReader(client.GetStream(), Encoding.UTF8, false, leaveOpen: true);
            using var writer = new StreamWriter(client.GetStream(), new UTF8Encoding(false), leaveOpen: true)
                { AutoFlush = true };
            var line = await reader.ReadLineAsync(cancellation.Token);
            var parts = line?.Split('|');
            if (parts is { Length: 3 } && parts[0] == "TICKETTOSS" && parts[1] == PairingCode &&
                parts[2].Equals("PING", StringComparison.OrdinalIgnoreCase))
            {
                await writer.WriteLineAsync("OK");
            }
            else if (parts is { Length: 3 } && parts[0] == "TICKETTOSS" && parts[1] == PairingCode &&
                Enum.TryParse<Outcome>(parts[2], true, out var outcome))
            {
                await writer.WriteLineAsync("OK");
                OutcomeReceived?.Invoke(outcome);
            }
            else
            {
                await writer.WriteLineAsync("DENIED");
            }
        }
    }

    private static string FindLocalAddress()
    {
        try
        {
            using var routeProbe = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, ProtocolType.Udp);
            routeProbe.Connect("8.8.8.8", 65530);
            if (routeProbe.LocalEndPoint is IPEndPoint endpoint) return endpoint.Address.ToString();
        }
        catch (SocketException) { }

        var address = NetworkInterface.GetAllNetworkInterfaces()
            .Where(adapter => adapter.OperationalStatus == OperationalStatus.Up &&
                              adapter.NetworkInterfaceType != NetworkInterfaceType.Loopback)
            .SelectMany(adapter => adapter.GetIPProperties().UnicastAddresses)
            .Select(item => item.Address)
            .FirstOrDefault(address => address.AddressFamily == AddressFamily.InterNetwork &&
                                       !IPAddress.IsLoopback(address));
        return address?.ToString() ?? "127.0.0.1";
    }

    private static string LoadOrCreatePairingCode()
    {
        var settingsFolder = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Ticket Toss Projector");
        var codeFile = Path.Combine(settingsFolder, "pairing-code.txt");

        try
        {
            if (File.Exists(codeFile))
            {
                var savedCode = File.ReadAllText(codeFile).Trim();
                if (savedCode.Length == 4 && savedCode.All(char.IsDigit)) return savedCode;
            }

            Directory.CreateDirectory(settingsFolder);
            var newCode = Random.Shared.Next(1000, 10000).ToString();
            File.WriteAllText(codeFile, newCode);
            return newCode;
        }
        catch (IOException)
        {
            // Pairing still works for this session if Windows blocks settings storage.
            return Random.Shared.Next(1000, 10000).ToString();
        }
        catch (UnauthorizedAccessException)
        {
            return Random.Shared.Next(1000, 10000).ToString();
        }
    }

    public void Dispose()
    {
        cancellation.Cancel();
        listener.Stop();
        cancellation.Dispose();
    }
}
