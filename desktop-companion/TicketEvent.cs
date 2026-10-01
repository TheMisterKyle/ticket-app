namespace TicketToss.Companion;

public sealed record TicketEvent(string Id, Outcome Outcome)
{
    public static bool TryParse(string[] parts, out TicketEvent? ticket)
    {
        ticket = null;
        if (parts.Length != 10 || parts[2] != "V1" ||
            !Guid.TryParseExact(parts[3], "D", out var id) || id.ToString() != parts[3] ||
            parts[4] is not ("GREEN" or "RED") || parts[5] is not ("0" or "1") ||
            !int.TryParse(parts[6], out var probability) ||
            !(parts[4] == "GREEN" ? new[] {15,35,60,80,100} : new[] {10,25,50,75,100}).Contains(probability) ||
            parts[7] is not ("PHONE" or "WATCH") ||
            (parts[8] != "PC_ONLY" && parts[8] != parts[7] + "_AND_PC") ||
            !long.TryParse(parts[9], out var timestamp) || timestamp <= 0) return false;
        var outcome = (parts[4], parts[5]) switch {
            ("GREEN", "1") => Outcome.GreenWin,
            ("GREEN", "0") => Outcome.GreenMiss,
            ("RED", "1") => Outcome.RedWin,
            _ => Outcome.RedMiss,
        };
        ticket = new TicketEvent(parts[3], outcome);
        return true;
    }
}

public sealed class EventReceiptCache
{
    private readonly Dictionary<string, (Outcome Outcome, DateTime Accepted)> receipts = new();
    public bool Accept(TicketEvent ticket, Action<Outcome> display)
    {
        lock (receipts)
        {
            // Longer than the bounded relay retry window; avoid unbounded classroom-session growth.
            var cutoff = DateTime.UtcNow.AddMinutes(-10);
            foreach (var key in receipts.Where(x => x.Value.Accepted < cutoff).Select(x => x.Key).ToArray())
                receipts.Remove(key);
            if (receipts.TryGetValue(ticket.Id, out var existing)) return existing.Outcome == ticket.Outcome;
            if (receipts.Count >= 4096) return false;
            display(ticket.Outcome);
            receipts.Add(ticket.Id, (ticket.Outcome, DateTime.UtcNow));
            return true;
        }
    }
}
