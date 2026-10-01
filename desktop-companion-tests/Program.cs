using System.Net.Sockets;
using TicketToss.Companion;

void Check(bool condition, string message) { if (!condition) throw new Exception(message); }
var id = Guid.NewGuid().ToString();
var payload = $"V1|{id}|GREEN|1|60|WATCH|PC_ONLY|1";
var code = "1234";
Check(TicketEvent.TryParse($"TICKETTOSS|{code}|{payload}".Split('|'), out var ticket), "valid event");
foreach (var bad in new[] { payload.Replace("V1", "V2"), payload.Replace("|60|", "|61|"), payload.Replace("|PC_ONLY|", "|WATCH_ONLY|"), payload + "|extra" })
    Check(!TicketEvent.TryParse($"TICKETTOSS|{code}|{bad}".Split('|'), out _), "reject malformed event");
var cache = new EventReceiptCache();
var count = 0;
Parallel.For(0, 30, _ => Check(cache.Accept(ticket!, _ => Interlocked.Increment(ref count)), "retry receipt"));
Check(count == 1, "duplicate displayed");
Check(!cache.Accept(ticket! with { Outcome = Outcome.RedWin }, _ => count++), "conflicting outcome");
Console.WriteLine("Protocol validation, exact outcomes and concurrent deduplication passed.");
if (args.Contains("--unit")) return;
using var server = new CompanionServer();
var outcomes = new List<Outcome>();
server.OutcomeReceived += outcome => { lock(outcomes) outcomes.Add(outcome); };
server.Start();
async Task<string?> Send(string line) {
    using var client = new TcpClient(); await client.ConnectAsync("127.0.0.1", CompanionServer.Port);
    using var writer = new StreamWriter(client.GetStream()) { AutoFlush = true };
    using var reader = new StreamReader(client.GetStream());
    await writer.WriteLineAsync(line); return await reader.ReadLineAsync();
}
code = server.PairingCode;
Check(await Send($"TICKETTOSS|{code}|PING") == "OK", "ping");
Check(await Send($"TICKETTOSS|0000|{payload}") == "DENIED", "pairing");
Check(await Send($"TICKETTOSS|{code}|{payload}") == $"OK|{id}", "event receipt");
Check(await Send($"TICKETTOSS|{code}|{payload}") == $"OK|{id}", "repeat receipt");
lock(outcomes) Check(outcomes.SequenceEqual(new[] { Outcome.GreenWin }), "repeat animation");
Check(await Send($"TICKETTOSS|{code}|999") == "DENIED", "undefined legacy enum");
Check(await Send(new string('x', 600)) == "DENIED", "oversized input");
foreach (var (colour, award, expected) in new[] { ("GREEN", "0", Outcome.GreenMiss), ("RED", "1", Outcome.RedWin), ("RED", "0", Outcome.RedMiss) }) {
    var nextId = Guid.NewGuid().ToString(); var probability = colour == "GREEN" ? 60 : 50;
    Check(await Send($"TICKETTOSS|{code}|V1|{nextId}|{colour}|{award}|{probability}|PHONE|PHONE_AND_PC|1") == $"OK|{nextId}", "exact outcome receipt");
    lock(outcomes) Check(outcomes.Last() == expected, "exact outcome");
}
Console.WriteLine("Protocol, TCP acknowledgements, exact outcomes, concurrent deduplication and malformed input checks passed.");
