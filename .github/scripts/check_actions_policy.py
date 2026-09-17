#!/usr/bin/env python3
from pathlib import Path
import re
import sys

WORKFLOWS = Path(".github/workflows")
errors = []

for path in sorted([*WORKFLOWS.glob("*.yml"), *WORKFLOWS.glob("*.yaml")]):
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()

    if path.name.startswith("apply-"):
        errors.append(f"{path}: one-off apply workflows must be removed after use")

    try:
        jobs_start = next(i for i, line in enumerate(lines) if line == "jobs:")
    except StopIteration:
        errors.append(f"{path}: missing jobs section")
        continue

    job_starts = []
    for i in range(jobs_start + 1, len(lines)):
        match = re.match(r"^  ([A-Za-z0-9_-]+):\s*$", lines[i])
        if match:
            job_starts.append((i, match.group(1)))

    for pos, (start, name) in enumerate(job_starts):
        end = job_starts[pos + 1][0] if pos + 1 < len(job_starts) else len(lines)
        block = "\n".join(lines[start:end])
        if re.search(r"^    runs-on:", block, re.M) and not re.search(r"^    timeout-minutes:\s*\d+", block, re.M):
            errors.append(f"{path}: job {name!r} is missing timeout-minutes")

    for match in re.finditer(r"(?m)^\s{6}-.*(?:\n(?!\s{6}-).*)*", text):
        block = match.group(0)
        if "actions/upload-artifact@" not in block:
            continue
        retention = re.search(r"retention-days:\s*(\d+)", block)
        if not retention:
            errors.append(f"{path}: upload-artifact step is missing retention-days")
        elif not 1 <= int(retention.group(1)) <= 7:
            errors.append(f"{path}: artifact retention must be between 1 and 7 days")

    on_match = re.search(r"(?ms)^on:\n(.*?)(?=^[A-Za-z][A-Za-z0-9_-]*:)", text)
    on_block = on_match.group(1) if on_match else ""
    has_push = re.search(r"(?m)^  push:", on_block) is not None
    has_pr = re.search(r"(?m)^  pull_request:", on_block) is not None
    bounded_push = "paths:" in on_block or "tags:" in on_block

    if has_push and "actions/upload-artifact@" in text and not bounded_push:
        errors.append(f"{path}: push-triggered artifact creation must be path- or tag-bounded")
    if has_push and "runs-on: windows-" in text and not bounded_push:
        errors.append(f"{path}: push-triggered Windows work must be path- or tag-bounded")
    if has_push and has_pr:
        if "cancel-in-progress: true" not in text:
            errors.append(f"{path}: push/PR validation must cancel superseded runs")

if errors:
    print("GitHub Actions governance violations:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("GitHub Actions governance policy passed.")
